package com.mylaesoftware;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GlobalConfigTypeReducerTest {

  interface TestInterface {

  }

  @Nested
  @DisplayName("Accumulator")
  class AccumulatorTest {

    @Test
    public void shouldReturnTypeWhenAccumulatedIsEmpty() {

    }

    @Test
    public void shouldReturnCombinedTypeWhenAccumulatedIsNotEmpty() {

    }

    @Test
    public void shouldThrowExceptionWhenElementIsNotAnInterface() {

    }
  }
}