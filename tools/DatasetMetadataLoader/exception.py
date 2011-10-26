class SynapseDeployerError(StandardError):
    '''
    Custom exception class for errors in Synapse deployment
    '''
    def __init__(self, reason, *args):
        StandardError.__init__(self, reason, *args)
        self.reason = reason

    def __repr__(self):
        return 'Synapse Deployment Error: %s' % self.reason

    def __str__(self):
        return 'Synapse Deployment Error: %s' % self.reason