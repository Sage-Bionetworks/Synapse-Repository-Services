You might run the following commands to delete all datasets and then repopulate them

# There are approx 114 datasets
curl http://repositoryservice.sagebase.org/repo/v1/dataset

./datasetNuker.py -e repositoryservice.sagebase.org --debug

# There are zero datasets
curl http://repositoryservice.sagebase.org/repo/v1/dataset

./datasetCsvLoader.py -e repositoryservice.sagebase.org --debug

# There are approx 114 datasets
curl http://repositoryservice.sagebase.org/repo/v1/dataset
