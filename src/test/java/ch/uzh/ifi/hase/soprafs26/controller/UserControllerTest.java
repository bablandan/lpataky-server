package ch.uzh.ifi.hase.soprafs26.controller;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;


import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs26.rest.dto.UserPutDTO;
import ch.uzh.ifi.hase.soprafs26.service.UserService;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import javax.print.attribute.standard.Media;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Test
	public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
		// given
		User user = new User();
		user.setName("Firstname Lastname");
		user.setUsername("firstname@lastname");
		user.setStatus(UserStatus.OFFLINE);

		List<User> allUsers = Collections.singletonList(user);

		Mockito.doNothing().when(userService).assertValidToken("token-1");

		// this mocks the UserService -> we define above what the userService should
		// return when getUsers() is called
		given(userService.getUsers()).willReturn(allUsers);

		// when
		MockHttpServletRequestBuilder getRequest = get("/users")
			.header("Authorization", "token-1")
			.contentType(MediaType.APPLICATION_JSON);

		// then
		mockMvc.perform(getRequest).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name", is(user.getName())))
				.andExpect(jsonPath("$[0].username", is(user.getUsername())))
				.andExpect(jsonPath("$[0].status", is(user.getStatus().toString())));
	}

	@Test
	public void createUser_validInput_userCreated() throws Exception {
		// given
		User user = new User();
		user.setId(1L);
		user.setName("Test User");
		user.setUsername("testUsername");
		user.setToken("1");
		user.setStatus(UserStatus.ONLINE);

		UserPostDTO userPostDTO = new UserPostDTO();
		userPostDTO.setName("Test User");
		userPostDTO.setUsername("testUsername");

		given(userService.createUser(Mockito.any())).willReturn(user);

		// when/then -> do the request + validate the result
		MockHttpServletRequestBuilder postRequest = post("/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content(asJsonString(userPostDTO));

		// then
		mockMvc.perform(postRequest)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", is(user.getId().intValue())))
				.andExpect(jsonPath("$.name", is(user.getName())))
				.andExpect(jsonPath("$.username", is(user.getUsername())))
				.andExpect(jsonPath("$.status", is(user.getStatus().toString())));
	}

	@Test
	public void createUser_invalid_Input_thenReturn409() throws Exception {
		// given
		User user = new User();
		user.setId(1L);
		user.setName("Test User");
		user.setUsername("testUsername");
		user.setToken("1");
		user.setStatus(UserStatus.ONLINE);
	}

	@Test
	public void givenUser_whenGetUserById_thenReturnJson() throws Exception {
		//given
		User user = new User();
		user.setId(1L);
		user.setName("Test User");
		user.setUsername("testUsername");
		user.setStatus(UserStatus.ONLINE);
		user.setBio("Hello!");
		user.setCreationDate(LocalDate.of(2026, 2, 28));


		Mockito.doNothing().when(userService).assertValidToken("token-1");
		//mocking the userService
		given(userService.getUser(user.getId())).willReturn(user);

		//when
		MockHttpServletRequestBuilder getRequest = get("/users/1")
			.header("Authorization", "token-1")
			.contentType(MediaType.APPLICATION_JSON);

		//then
		mockMvc.perform(getRequest)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id", is(1)))
			.andExpect(jsonPath("$.name", is("Test User")))
			.andExpect(jsonPath("$.username", is("testUsername")))
			.andExpect(jsonPath("$.status", is("ONLINE")))
			.andExpect(jsonPath("$.bio", is("Hello!")))
			.andExpect(jsonPath("$.creationDate", is("2026-02-28")));
		}

	@Test
	public void givenUnknownUser_whenGetUserById_thenReturn404() throws Exception {

		//Make auth check pass
		Mockito.doNothing().when(userService).assertValidToken("token-1");

		//given
		given(userService.getUser(99L))
			.willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		// when
		MockHttpServletRequestBuilder getRequest = get("/users/99")
			.header("Authorization", "token-1")
			.accept(MediaType.APPLICATION_JSON);

		// then
		mockMvc.perform(getRequest)
			.andExpect(status().isNotFound());
	}

	@Test
	public void givenUser_whenPutUserById_thenReturn204() throws Exception{
		
		// given
		UserPutDTO userPutDTO = new UserPutDTO();
		userPutDTO.setNewPassword("newSecurePassword");

		// Mock service: update succeeds
		Mockito.doNothing().when(userService)
			.updateUser(Mockito.eq(1L), Mockito.eq("token-1"), Mockito.any(UserPutDTO.class));

		// when
		MockHttpServletRequestBuilder putRequest = put("/users/1")
			.header("Authorization", "token-1")
			.contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.APPLICATION_JSON)
			.content(asJsonString(userPutDTO));

		// then
		mockMvc.perform(putRequest)
			.andExpect(status().isNoContent())
			.andExpect(content().string(""));
	}

	@Test
	public void givenUnknownUser_whenPutUser_thenReturn404() throws Exception {

		// given
		UserPutDTO userPutDTO = new UserPutDTO();
		userPutDTO.setNewPassword("newSecurePassword");

		Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
			.when(userService)
			.updateUser(Mockito.eq(99L), Mockito.eq("token-1"), Mockito.any(UserPutDTO.class));

		// when
		MockHttpServletRequestBuilder putRequest = put("/users/99")
			.header("Authorization", "token-1")
			.contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.APPLICATION_JSON)
			.content(asJsonString(userPutDTO));

		// then
		mockMvc.perform(putRequest)
			.andExpect(status().isNotFound());
	}
	/**
	 * Helper Method to convert userPostDTO into a JSON string such that the input
	 * can be processed
	 * Input will look like this: {"name": "Test User", "username": "testUsername"}
	 * 
	 * @param object
	 * @return string
	 */
	private String asJsonString(final Object object) {
		try {
			return new ObjectMapper().writeValueAsString(object);
		} catch (JacksonException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					String.format("The request body could not be created.%s", e.toString()));
		}
	}
}