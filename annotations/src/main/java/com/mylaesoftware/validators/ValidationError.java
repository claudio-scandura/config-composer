package com.mylaesoftware.validators;

public class ValidationError {

  protected final String errorMsg;

  @Override
  public String toString() {
    return errorMsg;
  }

  public ValidationError(String errorMsg) {
    this.errorMsg = errorMsg;
  }

  public WithClassInfo withClassInfo(Class<?> clazz) {
    return new WithClassInfo(errorMsg, clazz);
  }

  public WithFieldInfo withFieldInfo(Class<?> clazz, String fieldName) {
    return new WithFieldInfo(errorMsg, clazz, fieldName);
  }

  public class WithClassInfo extends ValidationError {
    private final Class<?> clazz;

    public WithClassInfo(String errorMsg, Class<?> clazz) {
      super(errorMsg);
      this.clazz = clazz;
    }

    @Override
    public String toString() {
      return String.format("Type: '%s', Error: '%s'", clazz, super.errorMsg);
    }
  }

  public class WithFieldInfo extends WithClassInfo {
    private final String fieldName;

    public WithFieldInfo(String errorMsg, Class<?> clazz, String fieldName) {
      super(errorMsg, clazz);
      this.fieldName = fieldName;
    }

    @Override
    public String toString() {
      return String.format("Type: '%s', Field: '%s', Error: '%s'", super.clazz, fieldName, super.errorMsg);
    }
  }
}
