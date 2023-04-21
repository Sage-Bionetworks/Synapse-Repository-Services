package controller;

import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BasicExampleController {
	ConcurrentMap<String, Integer> personNameToAge = new ConcurrentHashMap<>();
	
	/**
	 * This method returns the age of individual with name "name".
	 * 
	 * @param name - the name of the person
	 * @return the age of the person. If no record of person is found, then return 0.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/person/age/{name}", method = RequestMethod.GET)
	public @ResponseBody int getPersonAge(@PathVariable String name) {
		return personNameToAge.containsKey(name) ? personNameToAge.get(name) : 0;
	}
	
	/**
	 * Adds a person name along with their age to the data set.
	 * 
	 * @param name - the name of the person
	 * @param age - the age of the person
	 * @return the name of the person that was added
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/person/{name}", method = RequestMethod.POST)
	public @ResponseBody String addPerson(@PathVariable String name, @RequestBody int age) {
		personNameToAge.put(name, age);
		return name;
	}
}
