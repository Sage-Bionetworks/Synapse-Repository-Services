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
import org.sagebionetworks.openapi.pet.*;

/**
 * This controller is used to test translating for complex types.
 * @author lli
 *
 */
@ControllerInfo(displayName = "ComplexPets", path = "repo/v1")
public class ComplexExampleController {
	ConcurrentMap<String, Pet> petNameToPet = new ConcurrentHashMap<>();
	
	/**
	 * This method returns the Pet with 'name'.
	 * 
	 * @param name - the name of the pet
	 * @return the Pet associated with 'name'.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/complex-pet/{petName}", method = RequestMethod.GET)
	public @ResponseBody Pet getPet(@PathVariable String petName) {
		return petNameToPet.get(petName);
	}
	
	/**
	 * Adds a dog with name 'name'.
	 * 
	 * @param name - the name of the dog
	 * @param dog - the dog object to add
	 * @return the name of the Dog that was added
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/complex-pet/dog/{name}", method = RequestMethod.POST)
	public @ResponseBody String addDog(@PathVariable String name, @RequestBody Poodle dog) {
		petNameToPet.put(name, dog);
		return name;
	}
	
	/**
	 * Adds a cat with name 'name'.
	 * 
	 * @param name - the name of the cat
	 * @param cat - the cat to be added
	 * @return the name of the cat that was added
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/complex-pet/cat/{name}", method = RequestMethod.POST)
	public @ResponseBody String addCat(@PathVariable String name, @RequestBody Cat cat) {
		petNameToPet.put(name, cat);
		return name;
	}
}
