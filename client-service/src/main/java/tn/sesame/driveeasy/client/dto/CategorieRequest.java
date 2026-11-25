package tn.sesame.driveeasy.client.dto;

import jakarta.validation.constraints.NotNull;
import tn.sesame.driveeasy.client.entity.CategorieClient;

public record CategorieRequest(@NotNull(message = "La catégorie est obligatoire") CategorieClient categorieClient) {
}
