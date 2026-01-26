package devices.configuration.management;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VisibilityTest {

  @Test
  void shouldBeInaccessibleWhenViolationsExist() {
    // Given
    Violations violations =
        Violations.builder()
            .operatorNotAssigned(true)
            .providerNotAssigned(false)
            .locationMissing(false)
            .showOnMapButMissingLocation(false)
            .showOnMapButNoPublicAccess(false)
            .build();

    // When
    Visibility visibility = Visibility.calculateFrom(violations, true, true);

    // Then
    assertThat(visibility.forCustomer())
        .isEqualTo(Visibility.ForCustomer.INACCESSIBLE_AND_HIDDEN_ON_MAP);
    assertThat(visibility.roamingEnabled()).isFalse();
    assertThat(visibility.isInaccessible()).isTrue();
    assertThat(visibility.isUsable()).isFalse();
    assertThat(visibility.isVisibleOnMap()).isFalse();
  }

  @Test
  void shouldBeInaccessibleWhenNoPublicAccess() {
    // Given
    Violations violations =
        Violations.builder()
            .operatorNotAssigned(false)
            .providerNotAssigned(false)
            .locationMissing(false)
            .showOnMapButMissingLocation(false)
            .showOnMapButNoPublicAccess(false)
            .build();

    // When
    Visibility visibility = Visibility.calculateFrom(violations, false, true);

    // Then
    assertThat(visibility.forCustomer())
        .isEqualTo(Visibility.ForCustomer.INACCESSIBLE_AND_HIDDEN_ON_MAP);
    assertThat(visibility.roamingEnabled()).isFalse();
    assertThat(visibility.isInaccessible()).isTrue();
    assertThat(visibility.isUsable()).isFalse();
    assertThat(visibility.isVisibleOnMap()).isFalse();
  }

  @Test
  void shouldBeUsableAndVisibleOnMapWhenNoViolationsAndPublicAccessAndShowOnMap() {
    // Given
    Violations violations =
        Violations.builder()
            .operatorNotAssigned(false)
            .providerNotAssigned(false)
            .locationMissing(false)
            .showOnMapButMissingLocation(false)
            .showOnMapButNoPublicAccess(false)
            .build();

    // When
    Visibility visibility = Visibility.calculateFrom(violations, true, true);

    // Then
    assertThat(visibility.forCustomer())
        .isEqualTo(Visibility.ForCustomer.USABLE_AND_VISIBLE_ON_MAP);
    assertThat(visibility.roamingEnabled()).isTrue();
    assertThat(visibility.isUsable()).isTrue();
    assertThat(visibility.isVisibleOnMap()).isTrue();
    assertThat(visibility.isInaccessible()).isFalse();
  }

  @Test
  void shouldBeUsableButHiddenOnMapWhenNoViolationsAndPublicAccessButNotShowOnMap() {
    // Given
    Violations violations =
        Violations.builder()
            .operatorNotAssigned(false)
            .providerNotAssigned(false)
            .locationMissing(false)
            .showOnMapButMissingLocation(false)
            .showOnMapButNoPublicAccess(false)
            .build();

    // When
    Visibility visibility = Visibility.calculateFrom(violations, true, false);

    // Then
    assertThat(visibility.forCustomer()).isEqualTo(Visibility.ForCustomer.USABLE_BUT_HIDDEN_ON_MAP);
    assertThat(visibility.roamingEnabled()).isTrue();
    assertThat(visibility.isUsable()).isTrue();
    assertThat(visibility.isVisibleOnMap()).isFalse();
    assertThat(visibility.isInaccessible()).isFalse();
  }

  @Test
  void shouldBeInaccessibleWhenViolationsExistEvenIfPublicAccessAndShowOnMap() {
    // Given - multiple violations
    Violations violations =
        Violations.builder()
            .operatorNotAssigned(true)
            .providerNotAssigned(true)
            .locationMissing(true)
            .showOnMapButMissingLocation(false)
            .showOnMapButNoPublicAccess(false)
            .build();

    // When
    Visibility visibility = Visibility.calculateFrom(violations, true, true);

    // Then
    assertThat(visibility.forCustomer())
        .isEqualTo(Visibility.ForCustomer.INACCESSIBLE_AND_HIDDEN_ON_MAP);
    assertThat(visibility.roamingEnabled()).isFalse();
    assertThat(visibility.isInaccessible()).isTrue();
    assertThat(visibility.isUsable()).isFalse();
    assertThat(visibility.isVisibleOnMap()).isFalse();
  }

  @Test
  void shouldBeInaccessibleWhenNoPublicAccessEvenIfNoViolations() {
    // Given
    Violations violations =
        Violations.builder()
            .operatorNotAssigned(false)
            .providerNotAssigned(false)
            .locationMissing(false)
            .showOnMapButMissingLocation(false)
            .showOnMapButNoPublicAccess(false)
            .build();

    // When
    Visibility visibility = Visibility.calculateFrom(violations, false, false);

    // Then
    assertThat(visibility.forCustomer())
        .isEqualTo(Visibility.ForCustomer.INACCESSIBLE_AND_HIDDEN_ON_MAP);
    assertThat(visibility.roamingEnabled()).isFalse();
    assertThat(visibility.isInaccessible()).isTrue();
    assertThat(visibility.isUsable()).isFalse();
    assertThat(visibility.isVisibleOnMap()).isFalse();
  }

  @Test
  void shouldHaveRoamingEnabledOnlyWhenUsable() {
    // Given - usable device
    Violations noViolations =
        Violations.builder()
            .operatorNotAssigned(false)
            .providerNotAssigned(false)
            .locationMissing(false)
            .showOnMapButMissingLocation(false)
            .showOnMapButNoPublicAccess(false)
            .build();

    // When
    Visibility usableVisibility = Visibility.calculateFrom(noViolations, true, true);

    // Then
    assertThat(usableVisibility.roamingEnabled()).isTrue();

    // Given - not usable device
    Violations withViolations =
        Violations.builder()
            .operatorNotAssigned(true)
            .providerNotAssigned(false)
            .locationMissing(false)
            .showOnMapButMissingLocation(false)
            .showOnMapButNoPublicAccess(false)
            .build();

    // When
    Visibility notUsableVisibility = Visibility.calculateFrom(withViolations, true, true);

    // Then
    assertThat(notUsableVisibility.roamingEnabled()).isFalse();
  }

  @Test
  void shouldCalculateCorrectlyForAllCombinations() {
    // Test matrix: violations x publicAccess x showOnMap

    // No violations, public access, show on map -> USABLE_AND_VISIBLE_ON_MAP
    Violations noViolations =
        Violations.builder()
            .operatorNotAssigned(false)
            .providerNotAssigned(false)
            .locationMissing(false)
            .showOnMapButMissingLocation(false)
            .showOnMapButNoPublicAccess(false)
            .build();

    Visibility v1 = Visibility.calculateFrom(noViolations, true, true);
    assertThat(v1.forCustomer()).isEqualTo(Visibility.ForCustomer.USABLE_AND_VISIBLE_ON_MAP);
    assertThat(v1.roamingEnabled()).isTrue();

    // No violations, public access, no show on map -> USABLE_BUT_HIDDEN_ON_MAP
    Visibility v2 = Visibility.calculateFrom(noViolations, true, false);
    assertThat(v2.forCustomer()).isEqualTo(Visibility.ForCustomer.USABLE_BUT_HIDDEN_ON_MAP);
    assertThat(v2.roamingEnabled()).isTrue();

    // No violations, no public access, show on map ->
    // INACCESSIBLE_AND_HIDDEN_ON_MAP
    Visibility v3 = Visibility.calculateFrom(noViolations, false, true);
    assertThat(v3.forCustomer()).isEqualTo(Visibility.ForCustomer.INACCESSIBLE_AND_HIDDEN_ON_MAP);
    assertThat(v3.roamingEnabled()).isFalse();

    // No violations, no public access, no show on map ->
    // INACCESSIBLE_AND_HIDDEN_ON_MAP
    Visibility v4 = Visibility.calculateFrom(noViolations, false, false);
    assertThat(v4.forCustomer()).isEqualTo(Visibility.ForCustomer.INACCESSIBLE_AND_HIDDEN_ON_MAP);
    assertThat(v4.roamingEnabled()).isFalse();

    // With violations - always INACCESSIBLE_AND_HIDDEN_ON_MAP regardless of other
    // settings
    Violations withViolations =
        Violations.builder()
            .operatorNotAssigned(true)
            .providerNotAssigned(false)
            .locationMissing(false)
            .showOnMapButMissingLocation(false)
            .showOnMapButNoPublicAccess(false)
            .build();

    Visibility v5 = Visibility.calculateFrom(withViolations, true, true);
    assertThat(v5.forCustomer()).isEqualTo(Visibility.ForCustomer.INACCESSIBLE_AND_HIDDEN_ON_MAP);
    assertThat(v5.roamingEnabled()).isFalse();

    Visibility v6 = Visibility.calculateFrom(withViolations, true, false);
    assertThat(v6.forCustomer()).isEqualTo(Visibility.ForCustomer.INACCESSIBLE_AND_HIDDEN_ON_MAP);
    assertThat(v6.roamingEnabled()).isFalse();

    Visibility v7 = Visibility.calculateFrom(withViolations, false, true);
    assertThat(v7.forCustomer()).isEqualTo(Visibility.ForCustomer.INACCESSIBLE_AND_HIDDEN_ON_MAP);
    assertThat(v7.roamingEnabled()).isFalse();

    Visibility v8 = Visibility.calculateFrom(withViolations, false, false);
    assertThat(v8.forCustomer()).isEqualTo(Visibility.ForCustomer.INACCESSIBLE_AND_HIDDEN_ON_MAP);
    assertThat(v8.roamingEnabled()).isFalse();
  }
}
