package controller;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.sagebionetworks.repo.model.BooleanResult;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.principal.AccountSetupInfo;
import org.sagebionetworks.repo.model.oauth.OAuthTokenRevocationRequest;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.sagebionetworks.openapi.pet.*;
import org.springframework.web.util.UriComponentsBuilder;

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
	
	/**
	 * Example of an endpoint that would be redirected.
	 * 
	 * @param redirect if the endpoint will redirect the client
	 */
	@RequestMapping(value = "/complex-pet/redirected", method = RequestMethod.GET)
	public void redirected(@RequestBody Boolean redirect) {}

	/**
	 * Example of an endpoint that returns void but is not redirected.
	 *
	 * @param name
	 * 	the name of the pet
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/complex-pet/voidreturnnoredirect/{name}", method = RequestMethod.DELETE)
	public void deleteCat(@PathVariable String name) {}

	/**
	 * Example of an endpoint that returns an object but does not have a comment description for the return
	 *
	 * @param name
	 * 	the name of the pet
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/complex-pet/noreturndescription/{name}", method = RequestMethod.GET)
	public Pet getPetNoReturnDescription(@PathVariable String name) {}

	/**
	 * Example of an endpoint with an HttpServletResponse parameter, which does not have an annotation
	 *
	 * @param fileId
	 * 	the file for the pet
	 */
	@RequestMapping(value = "/complex-pet/file/{fileId}/url/httpservletresponse", method = RequestMethod.GET)
	public void getPetFileHandleURL(
			@PathVariable String fileId,
			@RequestParam(required = false) Boolean redirect,
			HttpServletResponse response) {}

	/**
	 * Example of an endpoint with an HttpServletRequest parameter, which does not have an annotation
	 *
	 * @param name
	 * 	the name for the dog
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/complex-pet/dog/{name}/httpservletrequest", method = RequestMethod.DELETE)
	public void deleteDog(
			@PathVariable String name,
			HttpServletRequest request) {}

	/**
	 * Example of an endpoint with an UriComponentsBuilder parameter, which does not have an annotation
	 *
	 * @param accountSetupInfo user's first name, last name, requested user name, password, and validation token
	 * @return an access token, allowing the client to begin making authenticated requests
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/complex-pet/account/uricomponentsbuilder", method = RequestMethod.POST)
	@ResponseBody
	public LoginResponse createNewAccount(
			@RequestBody AccountSetupInfo accountSetupInfo,
			UriComponentsBuilder uriComponentsBuilder) {}

	/**
	 * Example of an endpoint that uses an @RequestHeader annotation
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/complex-pet/requestheader", method = RequestMethod.POST)
	public void revokeToken(
			@RequestHeader(value = "testClientId", required=true) String verifiedClientId,
			@RequestBody OAuthTokenRevocationRequest revokeRequest) {}

	/**
	 * Example of an endpoint with a response status of 'NO_CONTENT'
	 *
	 * @param name a name
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/complex-pet/nocontentresponsestatus", method = RequestMethod.POST)
	public void getNoContentResponseStatus(@PathVariable String name) {}

	/**
	 * Example of an endpoint with a response status of 'ACCEPTED'
	 *
	 * @param name a name
	 * @return the name that was added
	 */
	@ResponseStatus(HttpStatus.ACCEPTED)
	@RequestMapping(value = "/complex-pet/acceptedresponsestatus", method = RequestMethod.POST)
	public @ResponseBody String getAcceptedResponseStatus(@PathVariable String name) {}

	/**
	 * Example of an endpoint with a response status of 'GONE'
	 *
	 * @param name a name
	 * @return the name that was added
	 */
	@ResponseStatus(HttpStatus.GONE)
	@RequestMapping(value = "/complex-pet/goneresponsestatus", method = RequestMethod.POST)
	public @ResponseBody String getGoneResponseStatus(@PathVariable String name) {}

	/**
	 * Example of an endpoint with no response status
	 *
	 * @param name a name
	 * @return the name that was added
	 */
	@RequestMapping(value = "/complex-pet/noresponsestatus", method = RequestMethod.POST)
	public @ResponseBody String getNoResponseStatus(@PathVariable String name) {}

	/**
	 * Example of a private method included in the controller
	 *
	 * @param wikiId
	 * @param wikiPage
	 */
	private void validateUpateArguments(String wikiId, WikiPage wikiPage) {
	}

	/**
	 * Example of a static method included in the controller
	 */
	public static void staticMethod() {
	}

	/**
	 * Example of an endpoint that has been deprecated
	 */
	@Deprecated
	@RequestMapping(value = "/complex-pet/deprecated", method = RequestMethod.GET)
	public void getDeprecated() {}

	/**
	 * Example of an endpoint where the method parameter name does not match the path variable name
	 *
	 * @param petName the name of the pet
	 * @return boolean on if the pet as a tail or not
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/complex-pet/differentpathandmethodparameternames/{petName}", method = RequestMethod.GET)
	public @ResponseBody Boolean doesPetHaveTail(
			@PathVariable(value = "petName") String nameOfPet) {
		return petNameToPet.get(petName).getHasTail();
	}

	/**
	 * Example of an endpoint with a regular expression in a path parameter
	 *
	 * @param id an id
	 * @return a string
	 */
	@RequestMapping(value = "/complex-pet/regularexpression/{id:.+}/test", method = RequestMethod.GET)
	public @ResponseBody String getRegularExpression(@PathVariable String id) {}

	/**
	 * Example of an endpoint that takes a string as a parameter and returns a string
	 * @return a string
	 */
	@RequestMapping(value = "/complex-pet/string/{testString}", method = RequestMethod.GET)
	public @ResponseBody String getString(@PathVariable String testString) {}

	/**
	 * Example of an endpoint that takes an integer object as a parameter and returns an integer object
	 * @return an integer object
	 */
	@RequestMapping(value = "/complex-pet/integerclass/{testIntegerClass}", method = RequestMethod.GET)
	public @ResponseBody Integer getIntegerClass(@PathVariable Integer testIntegerClass) {}

	/**
	 * Example of an endpoint that takes a boolean object as a parameter and returns a boolean object
	 * @return a boolean object
	 */
	@RequestMapping(value = "/complex-pet/booleanclass/{testBooleanClass}", method = RequestMethod.GET)
	public @ResponseBody Boolean getBooleanClass(@PathVariable Boolean testBooleanClass) {}

	/**
	 * Example of an endpoint that takes a long object as a parameter and returns a long object
	 * @return a long object
	 */
	@RequestMapping(value = "/complex-pet/longclass/{testLongClass}", method = RequestMethod.GET)
	public @ResponseBody Long getLongClass(@PathVariable Long testLongClass) {}

	/**
	 * Example of an endpoint that takes an integer primitive as a parameter and returns an integer primitive
	 * @return an integer
	 */
	@RequestMapping(value = "/complex-pet/intprimitive/{testIntPrimitive}", method = RequestMethod.GET)
	public @ResponseBody int getBooleanClass(@PathVariable int testIntPrimitive) {}

	/**
	 * Example of an endpoint that takes a boolean primitive as a parameter and returns a boolean primitive
	 * @return a boolean
	 */
	@RequestMapping(value = "/complex-pet/booleanprimitive/{testBooleanPrimitive}", method = RequestMethod.GET)
	public @ResponseBody boolean getBooleanClass(@PathVariable boolean testBooleanPrimitive) {}

	/**
	 * Example of an endpoint that takes a long primitive as a parameter and returns a long primitive
	 * @return a boolean
	 */
	@RequestMapping(value = "/complex-pet/longprimitive/{testLongPrimitive}", method = RequestMethod.GET)
	public @ResponseBody long getLongClass(@PathVariable long testLongPrimitive) {}

	/**
	 * Example of an endpoint that takes an object as a parameter and returns an object
	 * @return an object
	 */
	@RequestMapping(value = "/complex-pet/objectclass/{testObject}", method = RequestMethod.GET)
	public @ResponseBody Object getObjectClass(@PathVariable Object testObject) {}

	/**
	 * Example of an endpoint that returns a BooleanResult
	 * @return a BooleanResult
	 */
	@RequestMapping(value = "/complex-pet/booleanresult", method = RequestMethod.GET)
	public @ResponseBody BooleanResult getBooleanResult() {}

	/**
	 * Example of an endpoint that takes a JSONObject as a parameter and returns a JSONObject
	 * @return a JSONObject
	 */
	@RequestMapping(value = "/complex-pet/jsonobject/{testJsonObject}", method = RequestMethod.GET)
	public @ResponseBody JSONObject getJsonObject(@PathVariable JSONObject testJsonObject) {}

	/**
	 * Example of an endpoint that returns an ObjectSchema
	 * @return an ObjectSchema
	 */
	@RequestMapping(value = "/complex-pet/objectschema", method = RequestMethod.GET)
	public @ResponseBody ObjectSchema getObjectSchema() {}
}
