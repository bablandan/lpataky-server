package ch.uzh.ifi.hase.soprafs26.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cglib.core.Local;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs26.entity.User;
import ch.uzh.ifi.hase.soprafs26.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs26.rest.dto.PasswordChangeDTO;

import java.util.List;
import java.util.UUID;

import java.time.LocalDate;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

	private final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;

	public UserService(@Qualifier("userRepository") UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public List<User> getUsers() {
		return this.userRepository.findAll();
	}

	public User createUser(User newUser) {
		
		//checks that input is not blank space
		if (newUser.getName() == null || newUser.getName().isBlank()
    	|| newUser.getUsername() == null || newUser.getUsername().isBlank()
    	|| newUser.getPassword() == null || newUser.getPassword().isBlank()){
    	throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "Name, username, and password must not be blank.");
	}

		checkIfUserExists(newUser);
		newUser.setToken(UUID.randomUUID().toString());
		newUser.setStatus(UserStatus.OFFLINE);
		newUser.setCreationDate(LocalDate.now());
		// saves the given entity but data is only persisted in the database once
		// flush() is called
		newUser = userRepository.save(newUser);
		userRepository.flush();

		log.debug("Created Information for User: {}", newUser);
		return newUser;
	}

	/**
	 * This is a helper method that will check the uniqueness criteria of the
	 * username and the name
	 * defined in the User entity. The method will do nothing if the input is unique
	 * and throw an error otherwise.
	 *
	 * @param userToBeCreated
	 * @throws org.springframework.web.server.ResponseStatusException
	 * @see User
	 */
	private void checkIfUserExists(User userToBeCreated) {
		User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());

		String baseErrorMessage = "The %s provided %s not unique. Therefore, the user could not be created!";
		if (userByUsername != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(baseErrorMessage, "username", "is"));
		} 
	}


	//Method to login an existing user
	public User loginUser(String username, String password){
		
		if (username == null || username.isBlank()
		|| password == null || password.isBlank()){
		throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
		"Username or password is blank");
	}

		User user = userRepository.findByUsername(username);
	
		//Check if user exists
		if (user == null){
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
		}

		//Check if password matches
		if (!user.getPassword().equals(password)){
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
		}

		//Successful login
		user.setStatus(UserStatus.ONLINE);
	
		//New token
		user.setToken(UUID.randomUUID().toString());


		//Save changings
		user = userRepository.save(user);
		userRepository.flush();

		log.debug("User logged in: {}", user);
		return user;
	}

	//Method to logout a user
	public void logoutUser(String token) {
		if (token == null || token.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"Token is blank");
		}

		User user = userRepository.findByToken(token);

		//Check if user exists
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
		}
		
		//Successful logout
		user.setStatus(UserStatus.OFFLINE);

		userRepository.save(user);
		userRepository.flush();
		log.debug("User logged out: {}", user);
	}
	
	//Method to change password
	public void changePassword(String token, PasswordChangeDTO dto) {

		if (token == null || token.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
			"Token is blank");
		}

		User user = userRepository.findByToken(token);
		if (user == null){
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
		}

		if (dto.getNewPassword() == null || dto.getNewPassword().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
			"New password is blank"
			);
		}

		user.setPassword(dto.getNewPassword());
		userRepository.save(user);
		userRepository.flush();
		log.debug("Password updated: {}", user);

		logoutUser(token);
	}
	
	
}
