package com.xxAMIDOxx.xxSTACKSxx.menu.api.v1.impl;

import com.microsoft.azure.spring.autoconfigure.cosmosdb.CosmosAutoConfiguration;
import com.microsoft.azure.spring.autoconfigure.cosmosdb.CosmosDbRepositoriesAutoConfiguration;
import com.xxAMIDOxx.xxSTACKSxx.core.api.dto.ErrorResponse;
import com.xxAMIDOxx.xxSTACKSxx.menu.api.v1.dto.request.UpdateMenuRequest;
import com.xxAMIDOxx.xxSTACKSxx.menu.api.v1.dto.response.ResourceCreatedResponse;
import com.xxAMIDOxx.xxSTACKSxx.menu.domain.Menu;
import com.xxAMIDOxx.xxSTACKSxx.menu.repository.MenuRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Optional;
import java.util.UUID;

import static com.azure.data.cosmos.internal.Utils.randomUUID;
import static com.xxAMIDOxx.xxSTACKSxx.menu.domain.MenuHelper.createMenu;
import static com.xxAMIDOxx.xxSTACKSxx.util.TestHelper.getBaseURL;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(
        exclude = {
                CosmosDbRepositoriesAutoConfiguration.class,
                CosmosAutoConfiguration.class
        })
@Tag("Integration")
class UpdateMenuControllerImplTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @MockBean
    private MenuRepository menuRepository;

    @Test
    void testUpdateSuccess() {
        // Given
        Menu menu = createMenu(0);
        when(menuRepository.findById(eq(menu.getId())))
                .thenReturn(Optional.of(menu));

        UpdateMenuRequest request =
                new UpdateMenuRequest("new name", "new description", false);

        // When
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var requestEntity = new HttpEntity<>(request, headers);

        var response =
                this.testRestTemplate.exchange(
                        String.format("%s/v1/menu/%s", getBaseURL(port), menu.getId()),
                        HttpMethod.PUT,
                        requestEntity,
                        ResourceCreatedResponse.class);

        // Then
        ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
        verify(menuRepository, times(1)).save(captor.capture());
        Menu updated = captor.getValue();

        then(updated.getName()).isEqualTo(request.getName());
        then(updated.getDescription()).isEqualTo(request.getDescription());
        then(updated.getEnabled()).isEqualTo(request.getEnabled());
        then(updated.getRestaurantId()).isEqualTo(menu.getRestaurantId());

        then(response).isNotNull();
        then(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testCannotUpdateIfMenuDoesntExist() {
        // Given
        UUID menuId = randomUUID();
        when(menuRepository.findById(eq(menuId.toString())))
                .thenReturn(Optional.empty());

        UpdateMenuRequest request =
                new UpdateMenuRequest("name", "description", true);

        // When
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var requestEntity = new HttpEntity<>(request, headers);

        var response =
                this.testRestTemplate.exchange(
                        String.format("%s/v1/menu/%s", getBaseURL(port), menuId),
                        HttpMethod.PUT,
                        requestEntity,
                        ErrorResponse.class);

        // Then
        then(response).isNotNull();
        then(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
