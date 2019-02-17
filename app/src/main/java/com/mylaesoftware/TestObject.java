package com.mylaesoftware;

public class TestObject {

  final String fieldA;
  final String fieldB;

  public TestObject(String fieldA, String fieldB) {
    this.fieldA = fieldA;
    this.fieldB = fieldB;
  }

  @Override
  public String toString() {
    return fieldA + " " + fieldB;
  }
}
