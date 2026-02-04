package com.example.reservation.controller;

import com.example.reservation.TestcontainersConfiguration;
import com.example.reservation.config.TestSecurityConfig;
import com.example.reservation.domain.property.Property;
import com.example.reservation.domain.property.PropertyStatus;
import com.example.reservation.repository.PropertyRepository;
import com.example.reservation.security.WithJwt;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestSecurityConfig.class})
class PropertyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PropertyRepository propertyRepository;

    private static final String OWNER_SUB = "owner-user-sub";
    private static final String OTHER_USER_SUB = "other-user-sub";

    @BeforeEach
    void setUp() {
        propertyRepository.deleteAll();
    }

    private Property createProperty(String ownerSub, PropertyStatus status) {
        Property property = Property.builder()
                .ownerSub(ownerSub)
                .title("Test Property")
                .description("A beautiful test property")
                .city("Paris")
                .pricePerNight(new BigDecimal("100.00"))
                .status(status)
                .build();
        return propertyRepository.save(property);
    }

    // ===== GET /api/properties =====

    @Nested
    @DisplayName("GET /api/properties - List active properties")
    class GetActiveProperties {

        @Test
        @DisplayName("Returns active properties without authentication")
        void returnsActivePropertiesWithoutAuth() throws Exception {
            createProperty(OWNER_SUB, PropertyStatus.ACTIVE);
            createProperty(OWNER_SUB, PropertyStatus.INACTIVE);

            mockMvc.perform(get("/api/properties"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].status", is("ACTIVE")))
                    .andExpect(jsonPath("$.totalElements", is(1)));
        }

        @Test
        @DisplayName("Filters by city")
        void filtersByCity() throws Exception {
            Property paris = createProperty(OWNER_SUB, PropertyStatus.ACTIVE);
            Property lyon = Property.builder()
                    .ownerSub(OWNER_SUB)
                    .title("Lyon Property")
                    .description("Property in Lyon")
                    .city("Lyon")
                    .pricePerNight(new BigDecimal("80.00"))
                    .status(PropertyStatus.ACTIVE)
                    .build();
            propertyRepository.save(lyon);

            mockMvc.perform(get("/api/properties").param("city", "Paris"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].city", is("Paris")));
        }

        @Test
        @DisplayName("Returns empty list when no active properties")
        void returnsEmptyList() throws Exception {
            createProperty(OWNER_SUB, PropertyStatus.INACTIVE);

            mockMvc.perform(get("/api/properties"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }
    }

    // ===== GET /api/properties/{id} =====

    @Nested
    @DisplayName("GET /api/properties/{id} - Get property by ID")
    class GetPropertyById {

        @Test
        @DisplayName("Returns property without authentication")
        void returnsPropertyWithoutAuth() throws Exception {
            Property property = createProperty(OWNER_SUB, PropertyStatus.ACTIVE);

            mockMvc.perform(get("/api/properties/{id}", property.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(property.getId().toString())))
                    .andExpect(jsonPath("$.title", is("Test Property")))
                    .andExpect(jsonPath("$.city", is("Paris")));
        }

        @Test
        @DisplayName("Returns 404 for non-existent property")
        void returns404ForNonExistent() throws Exception {
            mockMvc.perform(get("/api/properties/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title", is("Ressource non trouvée")));
        }
    }

    // ===== GET /api/properties/mine =====

    @Nested
    @DisplayName("GET /api/properties/mine - Get my properties")
    class GetMyProperties {

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Returns properties owned by authenticated user")
        void returnsOwnedProperties() throws Exception {
            createProperty(OWNER_SUB, PropertyStatus.ACTIVE);
            createProperty(OWNER_SUB, PropertyStatus.INACTIVE);
            createProperty(OTHER_USER_SUB, PropertyStatus.ACTIVE);

            mockMvc.perform(get("/api/properties/mine"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements", is(2)));
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            mockMvc.perform(get("/api/properties/mine"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ===== POST /api/properties =====

    @Nested
    @DisplayName("POST /api/properties - Create property")
    class CreateProperty {

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Creates property successfully")
        void createsPropertySuccessfully() throws Exception {
            Map<String, Object> request = Map.of(
                    "title", "New Property",
                    "description", "A new test property",
                    "city", "Lyon",
                    "pricePerNight", 150.00
            );

            mockMvc.perform(post("/api/properties")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title", is("New Property")))
                    .andExpect(jsonPath("$.city", is("Lyon")))
                    .andExpect(jsonPath("$.ownerSub", is(OWNER_SUB)))
                    .andExpect(jsonPath("$.status", is("ACTIVE")));

            assertThat(propertyRepository.findAll()).hasSize(1);
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            Map<String, Object> request = Map.of(
                    "title", "New Property",
                    "description", "A new test property",
                    "city", "Lyon",
                    "pricePerNight", 150.00
            );

            mockMvc.perform(post("/api/properties")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Returns 400 for invalid request - missing title")
        void returns400ForMissingTitle() throws Exception {
            Map<String, Object> request = Map.of(
                    "description", "A new test property",
                    "city", "Lyon",
                    "pricePerNight", 150.00
            );

            mockMvc.perform(post("/api/properties")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title", is("Erreur de validation")));
        }

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Returns 400 for invalid price")
        void returns400ForInvalidPrice() throws Exception {
            Map<String, Object> request = Map.of(
                    "title", "New Property",
                    "description", "A new test property",
                    "city", "Lyon",
                    "pricePerNight", 0
            );

            mockMvc.perform(post("/api/properties")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ===== PUT /api/properties/{id} =====

    @Nested
    @DisplayName("PUT /api/properties/{id} - Update property")
    class UpdateProperty {

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Updates property successfully")
        void updatesPropertySuccessfully() throws Exception {
            Property property = createProperty(OWNER_SUB, PropertyStatus.ACTIVE);

            Map<String, Object> request = Map.of(
                    "title", "Updated Title",
                    "pricePerNight", 200.00
            );

            mockMvc.perform(put("/api/properties/{id}", property.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title", is("Updated Title")))
                    .andExpect(jsonPath("$.pricePerNight", is(200.00)));
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            Property property = createProperty(OWNER_SUB, PropertyStatus.ACTIVE);

            mockMvc.perform(put("/api/properties/{id}", property.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithJwt(subject = OTHER_USER_SUB)
        @DisplayName("Returns 403 for non-owner")
        void returns403ForNonOwner() throws Exception {
            Property property = createProperty(OWNER_SUB, PropertyStatus.ACTIVE);

            Map<String, Object> request = Map.of("title", "Hacked Title");

            mockMvc.perform(put("/api/properties/{id}", property.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Returns 404 for non-existent property")
        void returns404ForNonExistent() throws Exception {
            mockMvc.perform(put("/api/properties/{id}", UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isNotFound());
        }
    }

    // ===== POST /api/properties/{id}/activate =====

    @Nested
    @DisplayName("POST /api/properties/{id}/activate - Activate property")
    class ActivateProperty {

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Activates property successfully")
        void activatesPropertySuccessfully() throws Exception {
            Property property = createProperty(OWNER_SUB, PropertyStatus.INACTIVE);

            mockMvc.perform(post("/api/properties/{id}/activate", property.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("ACTIVE")));
        }

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Returns 409 when already active")
        void returns409WhenAlreadyActive() throws Exception {
            Property property = createProperty(OWNER_SUB, PropertyStatus.ACTIVE);

            mockMvc.perform(post("/api/properties/{id}/activate", property.getId()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail", containsString("déjà active")));
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            Property property = createProperty(OWNER_SUB, PropertyStatus.INACTIVE);

            mockMvc.perform(post("/api/properties/{id}/activate", property.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithJwt(subject = OTHER_USER_SUB)
        @DisplayName("Returns 403 for non-owner")
        void returns403ForNonOwner() throws Exception {
            Property property = createProperty(OWNER_SUB, PropertyStatus.INACTIVE);

            mockMvc.perform(post("/api/properties/{id}/activate", property.getId()))
                    .andExpect(status().isForbidden());
        }
    }

    // ===== POST /api/properties/{id}/deactivate =====

    @Nested
    @DisplayName("POST /api/properties/{id}/deactivate - Deactivate property")
    class DeactivateProperty {

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Deactivates property successfully")
        void deactivatesPropertySuccessfully() throws Exception {
            Property property = createProperty(OWNER_SUB, PropertyStatus.ACTIVE);

            mockMvc.perform(post("/api/properties/{id}/deactivate", property.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("INACTIVE")));
        }

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Returns 409 when already inactive")
        void returns409WhenAlreadyInactive() throws Exception {
            Property property = createProperty(OWNER_SUB, PropertyStatus.INACTIVE);

            mockMvc.perform(post("/api/properties/{id}/deactivate", property.getId()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail", containsString("déjà inactive")));
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            Property property = createProperty(OWNER_SUB, PropertyStatus.ACTIVE);

            mockMvc.perform(post("/api/properties/{id}/deactivate", property.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithJwt(subject = OTHER_USER_SUB)
        @DisplayName("Returns 403 for non-owner")
        void returns403ForNonOwner() throws Exception {
            Property property = createProperty(OWNER_SUB, PropertyStatus.ACTIVE);

            mockMvc.perform(post("/api/properties/{id}/deactivate", property.getId()))
                    .andExpect(status().isForbidden());
        }
    }

    // ===== DELETE /api/properties/{id} =====

    @Nested
    @DisplayName("DELETE /api/properties/{id} - Delete property")
    class DeleteProperty {

        @Test
        @WithJwt(subject = OWNER_SUB)
        @DisplayName("Deletes property successfully")
        void deletesPropertySuccessfully() throws Exception {
            Property property = createProperty(OWNER_SUB, PropertyStatus.ACTIVE);

            mockMvc.perform(delete("/api/properties/{id}", property.getId()))
                    .andExpect(status().isNoContent());

            assertThat(propertyRepository.findById(property.getId())).isEmpty();
        }

        @Test
        @DisplayName("Returns 401 without authentication")
        void returns401WithoutAuth() throws Exception {
            Property property = createProperty(OWNER_SUB, PropertyStatus.ACTIVE);

            mockMvc.perform(delete("/api/properties/{id}", property.getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithJwt(subject = OTHER_USER_SUB)
        @DisplayName("Returns 403 for non-owner")
        void returns403ForNonOwner() throws Exception {
            Property property = createProperty(OWNER_SUB, PropertyStatus.ACTIVE);

            mockMvc.perform(delete("/api/properties/{id}", property.getId()))
                    .andExpect(status().isForbidden());

            assertThat(propertyRepository.findById(property.getId())).isPresent();
        }
    }
}
