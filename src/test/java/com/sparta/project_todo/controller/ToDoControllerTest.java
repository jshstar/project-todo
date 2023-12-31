package com.sparta.project_todo.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.project_todo.global.config.WebSecurityConfig;
import com.sparta.project_todo.todocard.dto.CompleteToDoResponseDto;
import com.sparta.project_todo.todocard.dto.GetAllToDoResponseDto;
import com.sparta.project_todo.todocard.dto.HiddenToDoResponseDto;
import com.sparta.project_todo.todocard.dto.SelectToDoResponseDto;
import com.sparta.project_todo.todocard.dto.ToDoPageCardListResponseDto;
import com.sparta.project_todo.todocard.dto.ToDoRequestDto;
import com.sparta.project_todo.todocard.dto.ToDoResponseDto;
import com.sparta.project_todo.todocard.entity.ToDoCard;
import com.sparta.project_todo.user.entity.User;
import com.sparta.project_todo.user.entity.UserRoleEnum;
import com.sparta.project_todo.security.MockSpringSecurityFilter;
import com.sparta.project_todo.security.UserDetailsImpl;
import com.sparta.project_todo.todocard.service.ToDoService;
import com.sparta.project_todo.todocard.controller.ToDoController;

@MockBean(JpaMetamodelMappingContext.class)
@WebMvcTest(
	controllers = {ToDoController.class },
	excludeFilters = {
		@ComponentScan.Filter(
			type = FilterType.ASSIGNABLE_TYPE,
			classes = WebSecurityConfig.class
		)
	}
)
class ToDoControllerTest {

	private MockMvc mvc;

	private Principal mockPrincipal;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private ToDoService toDoService;


	@BeforeEach
	public void setup() {
		mvc = MockMvcBuilders.webAppContextSetup(context)
			.apply(springSecurity(new MockSpringSecurityFilter()))
			.alwaysDo(print())
			.build();
	}

	private User mockUserSetup(String num) {
		// Mock 테스트 유져 생성
		String username = "testuser" + num;
		String password = "12345678" + num;
		UserRoleEnum role = UserRoleEnum.USER;
		User testUser = new User(username, password, role);
		UserDetailsImpl testUserDetails = new UserDetailsImpl(testUser);
		mockPrincipal =
			new UsernamePasswordAuthenticationToken(testUserDetails, "", testUserDetails.getAuthorities());
		return testUser;
	}

	@Test
	void 카드_작성테스트() throws Exception {
		//given
		User user = this.mockUserSetup("1");
		ToDoRequestDto toDoRequestDto = new ToDoRequestDto("title", "contents");
		ToDoCard toDoCard = new ToDoCard(toDoRequestDto, user);
		ToDoResponseDto toDoResponseDto = new ToDoResponseDto(toDoCard);


		String cardInfo = objectMapper.writeValueAsString(toDoRequestDto);

		given(toDoService.createCard(any(ToDoRequestDto.class),any(User.class))).willReturn(toDoResponseDto);


		// when
		mvc.perform(post("/api/todo")
				.contentType(MediaType.APPLICATION_JSON)
				.content(cardInfo)
				.accept(MediaType.APPLICATION_JSON)
				.principal(mockPrincipal)
			)
		//then
			.andExpect(status().isCreated())
			.andDo(print())
			.andExpect(jsonPath("$.data.title").value(toDoResponseDto.getTitle()))
			.andExpect(jsonPath("$.data.contents").value(toDoResponseDto.getContents()))
			.andExpect(jsonPath("$.data.username").value(toDoResponseDto.getUsername()));
	}

	@Nested
	class 카드조회_테스트{
		@Test
		void 카드조회_성공테스트() throws Exception {
			//given
			User user = mockUserSetup("1");
			ToDoRequestDto toDoRequestDto1 = new ToDoRequestDto("title1", "content1");
			ToDoRequestDto toDoRequestDto2 = new ToDoRequestDto("title2", "content2");
			ToDoCard toDoCard1 = new ToDoCard(toDoRequestDto1, user);
			ToDoCard todoCard2 = new ToDoCard(toDoRequestDto2, user);

			List<ToDoCard> toDoCardList = new ArrayList<>();
			toDoCardList.add(toDoCard1);
			toDoCardList.add(todoCard2);
			Page<ToDoCard> toDoCardPage = new PageImpl<>(toDoCardList);
			ToDoPageCardListResponseDto responseDto = new ToDoPageCardListResponseDto(toDoCardPage);

			given(toDoService.getPageCard(any(Pageable.class),any(UserDetailsImpl.class))).willReturn(responseDto);

			//when
			mvc.perform(get("/api/todo")
					.accept(MediaType.APPLICATION_JSON)
					.principal(mockPrincipal)
				)
			//then
				.andExpect(status().isOk())
				.andDo(print())
				.andExpect(jsonPath("$.data.pageList.content[0].title")
					.value(responseDto.getPageList().getContent().get(0).getTitle()))
				.andExpect(jsonPath("$.data.pageList.content[0].username")
					.value(responseDto.getPageList().getContent().get(0).getUsername()))
				.andExpect(jsonPath("$.data.pageList.content[1].title")
					.value(responseDto.getPageList().getContent().get(1).getTitle()))
				.andExpect(jsonPath("$.data.pageList.content[1].username")
					.value(responseDto.getPageList().getContent().get(1).getUsername()));
		}

		@Test
		void 카드목록_빈리스트일때_조회_실패테스트() throws Exception {
			//given
			mockUserSetup("1");
			List<ToDoCard> toDoCardList = new ArrayList<>();
			Page<ToDoCard> toDoCardPage = new PageImpl<>(toDoCardList);
			ToDoPageCardListResponseDto responseDto = new ToDoPageCardListResponseDto(toDoCardPage);

			given(toDoService.getPageCard(any(Pageable.class),any(UserDetailsImpl.class))).willReturn(responseDto);

			//when
			mvc.perform(get("/api/todo")
					.accept(MediaType.APPLICATION_JSON)
					.principal(mockPrincipal)
				)
			//then
				.andExpect(jsonPath("$.data.pageList.content.size()").value(0))
				.andExpect(status().isOk())
				.andDo(print());
		}
	}


	@Test
	void 카드목록_제목조회_성공테스트() throws Exception {
		//given
		User user = mockUserSetup("1");

		ToDoRequestDto toDoRequestDto1 = new ToDoRequestDto("answerTitle1", "content1");
		ToDoRequestDto toDoRequestDto2 = new ToDoRequestDto("answerTitle1", "content2");
		ToDoRequestDto toDoRequestDto3 = new ToDoRequestDto("noAnswer", "content2");

		ToDoCard toDoCard1 = new ToDoCard(toDoRequestDto1, user);
		ToDoCard todoCard2 = new ToDoCard(toDoRequestDto2, user);
		ToDoCard todoCard3 = new ToDoCard(toDoRequestDto3, user);

		List<GetAllToDoResponseDto> inputResponse = new ArrayList<>();
		inputResponse.add(new GetAllToDoResponseDto(toDoCard1));
		inputResponse.add(new GetAllToDoResponseDto(todoCard2));
		inputResponse.add(new GetAllToDoResponseDto(todoCard3));
		List<GetAllToDoResponseDto> answerResponse = new ArrayList<>();
		for (int i = 0; i < inputResponse.size(); i++) {
			if(inputResponse.get(i).getTitle().contains("Title"))
			{
				answerResponse.add(inputResponse.get(i));
			}
		}
		given(toDoService.getTitleCards(any(String.class),any(UserDetailsImpl.class))).willReturn(answerResponse);
		//when
		mvc.perform(get("/api/todo/title")
			.param("title","Title")
			.accept(MediaType.APPLICATION_JSON)
			.principal(mockPrincipal)
			)
		//then
			.andExpect(jsonPath("$.data.size()").value(2))
			.andExpect(jsonPath("$.data[0].title").value(answerResponse.get(0).getTitle()))
			.andExpect(jsonPath("$.data[1].title").value(answerResponse.get(1).getTitle()))
			.andExpect(jsonPath("$.data[0].username").value(answerResponse.get(0).getUsername()))
			.andExpect(status().isOk())
			.andDo(print());

	}


	@Test
	void 카드_업데이트_성공테스트() throws Exception {
		//given
		User user1 = mockUserSetup("1");
		ToDoRequestDto toDoRequestDto = new ToDoRequestDto("title", "updateContents");
		ToDoCard inputToDoCard1 = new ToDoCard(toDoRequestDto,user1);
		ReflectionTestUtils.setField(inputToDoCard1,"id",1L);

		ToDoRequestDto updateRequestDto = new ToDoRequestDto("updateTitle","updateContents");
		ToDoCard resultToDoCard = new ToDoCard(updateRequestDto,user1);

		SelectToDoResponseDto result = new SelectToDoResponseDto(resultToDoCard);
		given(toDoService.updateCard(any(Long.class), any(ToDoRequestDto.class),any(User.class))).willReturn(result);

		String cardInfo = objectMapper.writeValueAsString(toDoRequestDto);

		//when
		mvc.perform(put("/api/todo/{id}",  1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(cardInfo)
				.accept(MediaType.APPLICATION_JSON)
				.principal(mockPrincipal)
			)
			//then
			.andExpect(status().isOk())
			.andDo(print())
			.andExpect(jsonPath("$.data.title").value(result.getTitle()))
			.andExpect(jsonPath("$.data.contents").value(result.getContents()))
			.andExpect(jsonPath("$.data.username").value(result.getUsername()));

	}




	@Test
	void 카드완료_응답테스트() throws Exception {
		// given
		User user = mockUserSetup("1");

		ToDoRequestDto toDoRequestDto = new ToDoRequestDto("title", "contents");
		ToDoCard toDoCard = new ToDoCard(toDoRequestDto,user);
		ReflectionTestUtils.setField(toDoCard,"id",1L);
		toDoCard.complete(true);
		CompleteToDoResponseDto result = new CompleteToDoResponseDto(toDoCard);

		given(toDoService.completeCard(any(Long.class),any(User.class))).willReturn(result);

		String cardInfo = objectMapper.writeValueAsString(toDoRequestDto);
		//when
		mvc.perform(put("/api/todo/{id}/complete",1L)
			.contentType(MediaType.APPLICATION_JSON)
			.content(cardInfo)
			.accept(MediaType.APPLICATION_JSON)
			.principal(mockPrincipal)
			)
		//then
			.andExpect(jsonPath("$.data.id").value(result.getId()))
			.andExpect(jsonPath("$.data.complete").value(result.isComplete()))
			.andExpect(status().isOk())
			.andDo(print());
	}

	@Test
	void 카드히든_응답테스트() throws Exception {
		// given
		User user = mockUserSetup("1");

		ToDoRequestDto toDoRequestDto = new ToDoRequestDto("title", "contents");
		ToDoCard toDoCard = new ToDoCard(toDoRequestDto,user);
		ReflectionTestUtils.setField(toDoCard,"id",1L);
		toDoCard.hiddenFlag(true);
		HiddenToDoResponseDto result = new HiddenToDoResponseDto(toDoCard);

		given(toDoService.hiddenCard(any(Long.class),any(User.class))).willReturn(result);

		String cardInfo = objectMapper.writeValueAsString(toDoRequestDto);
		//when
		mvc.perform(put("/api/todo/{id}/hidden",1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(cardInfo)
				.accept(MediaType.APPLICATION_JSON)
				.principal(mockPrincipal)
			)
			//then
			.andExpect(jsonPath("$.data.id").value(result.getId()))
			.andExpect(jsonPath("$.data.hidden").value(result.isHidden()))
			.andExpect(status().isOk())
			.andDo(print());
	}

	@Test
	void 카드_유저불일치_예외응답_테스트() throws Exception {
		// 유저검색이 들어간 모든곳 에서 사용 따라서 대표적인 하나의 메서드에서만 사용
		//given
		User user1 = mockUserSetup("1");
		given(toDoService.updateCard(any(Long.class),any(ToDoRequestDto.class),any(User.class)))
			.willThrow(IllegalArgumentException.class);
		//when
		mvc.perform(put("/api/todo/{id}",  1L)
				.principal(mockPrincipal)
			)
			//then
			.andExpect(status().isBadRequest())
			.andDo(print());
	}


}
