
package com.ga.csvprocessor.model;

public enum EmployeeRole {
    DIRECTOR(5),
    MANAGER(2),
    EMPLOYEE(1);

    private final int baseIncrementPercentage;
    EmployeeRole(int baseIncrementPercentage) {
        this.baseIncrementPercentage = baseIncrementPercentage;
    }
    public int getBaseIncrementPercentage() {
        return baseIncrementPercentage;
    }
}
