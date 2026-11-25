package tn.sesame.driveeasy.location.client;

import java.util.List;

public record EligibiliteInfo(Long clientId, boolean eligible, int age, boolean permisValide,
                              String categorieClient, List<String> motifs) {
}
