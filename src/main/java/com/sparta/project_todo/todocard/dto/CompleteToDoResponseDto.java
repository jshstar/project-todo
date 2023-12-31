package com.sparta.project_todo.todocard.dto;

import com.sparta.project_todo.todocard.entity.ToDoCard;
import lombok.Getter;

@Getter
public class CompleteToDoResponseDto {
    private Long id; // 카드 id

    private boolean complete; // 완료 여부

    public CompleteToDoResponseDto(ToDoCard card){
        this.id = card.getId();
        this.complete = card.isComplete();
    }
}
