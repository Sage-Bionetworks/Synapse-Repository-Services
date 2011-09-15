# TODO: Add comment
# 
# Author: furia
###############################################################################


.setUp <- 
		function()
{
	## make a copy of the old cache
	oldCache <- synapseClient:::.cache
	newCache <- new.env(parent=parent.env(oldCache))
	for(key in ls(oldCache))
		assign(key,get(key,envir=oldCache), envir=newCache)
	
	myGetEntity <-
			function(entity)
	{
		list(name="SageBioCurationEula",
				annotations="/repo/v1/fakeType/3732/annotations",
				id="-12345",
				parentId="3730",
				etag="0",
				agreement="do you agree?",
				uri="/repo/v1/fakeType/3732",
				accessControlList="/repo/v1/fakeType/3732/acl"
		)
	}
	
	mySynapseEntity <-
			function(entity)
	{
		s4Entity <- new("SynapseEntity")
		synapseClient:::.populateSlotsFromEntity(s4Entity, entity)
	}
	
	unloadNamespace('synapseClient')
	assignInNamespace("SynapseEntity", mySynapseEntity, "synapseClient")
	assignInNamespace(".getEntity", myGetEntity, "synapseClient")
	assignInNamespace(".cache", newCache, "synapseClient")
	attachNamespace("synapseClient")
	synapseClient:::.setCache("oldCache", oldCache)
}

.tearDown <-
		function()
{
	oldCache <- synapseClient:::.getCache("oldCache")
	# put back the overridden functions and original cache
	unloadNamespace("synapseClient")
	assignInNamespace(".cache", oldCache, "synapseClient")
	attachNamespace("synapseClient")
}

unitTestGenericEntity <-
		function()
{
	entity <- getEntity("aFakeId")
	checkEquals("SynapseEntity", as.character(class(entity)))
}
