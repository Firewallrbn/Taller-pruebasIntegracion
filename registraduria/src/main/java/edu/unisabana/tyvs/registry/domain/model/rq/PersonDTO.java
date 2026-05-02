package edu.unisabana.tyvs.registry.domain.model.rq;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

public class PersonDTO {

    @NotBlank(message = "name is required")
    private String name;

    @Min(value = 1, message = "id must be greater than 0")
    private int id;

    @Min(value = 0, message = "age must be non-negative")
    private int age;

    @NotBlank(message = "gender is required")
    private String gender;

    private boolean alive;

    public PersonDTO() {
    }

    public PersonDTO(String name, int id, int age, String gender, boolean alive) {
        this.name = name;
        this.id = id;
        this.age = age;
        this.gender = gender;
        this.alive = alive;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public int getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }

    public boolean isAlive() {
        return alive;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }
}
