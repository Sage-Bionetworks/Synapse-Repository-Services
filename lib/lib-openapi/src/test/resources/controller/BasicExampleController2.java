package controller;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 *  This exists to test the multiple controller case.
 * @author lli
 *
 */
@ControllerInfo(displayName = "Pet", path = "repo/v1")
public class BasicExampleController2 {
	ConcurrentMap<String, Integer> personNameToPets = new ConcurrentHashMap<>();
	
	/**
	 * This method returns the nunmber of pets the individual with name "name" has.
	 * 
	 * @param name - the name of the person
	 * @return the number of pets this person has, default to 0 if name of person does not exist.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/pet/num-pets/{name}", method = RequestMethod.GET)
	public @ResponseBody int getNumPets(@PathVariable String name) {
		return personNameToPets.containsKey(name) ? personNameToPets.get(name) : 0;
	}
	
	/**
	 * Adds a person name along with their number of pets to the dataset.
	 * 
	 * @param name - the name of the person
	 * @param numPets - the number of pets this person has
	 * @return the name of the person that was added
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/pet/{name}", method = RequestMethod.POST)
	public @ResponseBody String addPets(@PathVariable String name, @RequestBody int numPets) {
		personNameToPets.put(name, numPets);
		return name;
	}
}
