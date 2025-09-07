package com.example.habibitar.domain.model;

import com.example.habibitar.domain.model.enums.Difficulty;
import com.example.habibitar.domain.model.enums.Frequency;
import com.example.habibitar.domain.model.enums.Importance;
import com.example.habibitar.domain.model.enums.RepetitionUnits;
import com.example.habibitar.domain.model.enums.TaskStatus;
import com.google.firebase.firestore.Exclude;

import java.util.Date;

public class Task {
    private String id;
    private String ownerId;
    private String categoryId;
    @Exclude
    private Category category;
    private Frequency frequency;
    private Difficulty difficulty;
    private Importance importance;
    private String name;
    private String description;
    private Date createdAt;
    private Integer repeatingInterval;
    private RepetitionUnits unitOfRepetition;
    private Date repetitionStartDate;
    private Date repetitionEndDate;
    private TaskStatus status;

    public Task() { }

    public Task(Task task) {
        this.id = task.id;
        this.ownerId = task.ownerId;
        this.categoryId = task.categoryId;
        this.category = task.category;
        this.frequency = task.frequency;
        this.difficulty = task.difficulty;
        this.importance = task.importance;
        this.name = task.name;
        this.description = task.description;
        this.status = TaskStatus.ACTIVE;
    }

    public Task(String id, String ownerId, String categoryId, String name, String description)
    {
        this.id = id;
        this.ownerId = ownerId;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.status = TaskStatus.ACTIVE;
    }

    public Task(String categoryId,
                String name,
                String description,
                Difficulty difficulty,
                Importance importance,
                Frequency frequency,
                Integer repeatingInterval,
                RepetitionUnits unitOfRepetition,
                Date repetitionStartDate,
                Date repetitionEndDate)
    {
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.createdAt = new Date();
        this.difficulty = difficulty;
        this.importance = importance;
        this.frequency = frequency;
        this.repeatingInterval = repeatingInterval;
        this.unitOfRepetition = unitOfRepetition;
        this.repetitionStartDate = repetitionStartDate;
        this.repetitionEndDate = repetitionEndDate;
        this.status = TaskStatus.ACTIVE;
    }

    @Exclude
    public String getCategoryName() {
        return (category != null && category.getName() != null) ? category.getName() : null; 
    }

    @Exclude
    public String getCategoryColorCode() {
        return (category != null && category.getColorCode() != null) ? category.getColorCode() : null; 
    }

    public TaskStatus getStatus()
    {
        return status;
    }

    public void setStatus(TaskStatus status)
    {
        this.status = status;
    }

    public Date getRepetitionStart()
    {
        return repetitionStartDate;
    }

    public Date getRepetitionEnd()
    {
        return repetitionEndDate;
    }


    public Date getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(Date newDate)
    {
        this.createdAt = newDate;
    }


    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getCategoryId() {
        return categoryId;
    }
    @Exclude

    public Category getCategory() {
        return category;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public Importance getImportance() {
        return importance;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setFrequency(Frequency frequency) {
        this.frequency = frequency;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public void setImportance(Importance importance) {
        this.importance = importance;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getRepeatingInterval() {
        return repeatingInterval;
    }

    public void setRepeatingInterval(Integer repeatingInterval) {
        this.repeatingInterval = repeatingInterval;
    }

    public RepetitionUnits getUnitOfRepetition() {
        return unitOfRepetition;
    }

    public void setUnitOfRepetition(RepetitionUnits unitOfRepetition) {
        this.unitOfRepetition = unitOfRepetition;
    }

    public Date getRepetitionStartDate() {
        return repetitionStartDate;
    }

    public void setRepetitionStartDate(Date repetitionStartDate) {
        this.repetitionStartDate = repetitionStartDate;
    }

    public Date getRepetitionEndDate() {
        return repetitionEndDate;
    }

    public void setRepetitionEndDate(Date repetitionEndDate) {
        this.repetitionEndDate = repetitionEndDate;
    }

}
