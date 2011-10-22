use LWP::Simple;

# Download PubMed records for inputted platform
my %platforms;
open F, "src/main/resources/ncbiGPLIDs" or die("dead");
while (<F>) {
	chomp;
	$platforms{$_} = 1;
}

my %gses;
system("rm all.GSEs.txt");

open O, ">>all.GSEs.txt";
print O
"GSE.ID\tGPL\tLast Update Date\tSpecies\tSummary\tSupplementary_File\tN_Samples\n";
close O;

foreach my $p ( keys %platforms ) {
	print "Downloading information for $p\n";
	open G, ">output";
	$db    = 'gds';
	$query = $p . '[ACCN]+AND+gse[ETYP]&usehistory=y';

	#assemble the esearch URL
	$base = 'http://eutils.ncbi.nlm.nih.gov/entrez/eutils/';
	$url  = $base . "esearch.fcgi?db=$db&term=$query&usehistory=y";

	#post the esearch URL
	$output = get($url);

	#parse WebEnv and QueryKey
	$web = $1 if ( $output =~ /<WebEnv>(\S+)<\/WebEnv>/ );
	$key = $1 if ( $output =~ /<QueryKey>(\d+)<\/QueryKey>/ );

	### include this code for ESearch-ESummary
	#assemble the esummary URL
	$url = $base . "esummary.fcgi?db=$db&query_key=$key&WebEnv=$web";

	#post the esummary URL
	$docsums = get($url);
	print G "$docsums";

	### include this code for ESearch-EFetch
	#assemble the efetch URL
	$url = $base . "efetch.fcgi?db=$db&query_key=$key&WebEnv=$web";
	$url .= "&rettype=abstract&retmode=text";

	#post the efetch URL
	$data = get($url);
	print G "$data";

	#close File.
	close G;

	#open File and extract information of interest
	my $info = getInfo("output");
	system('grep -e "Item Name=\"GSE\"" output  > hmm');
	open F, "hmm";
	while (<F>) {
		if (/\d+/) {
			print O $p, "\t", 'GSE' . $&, "\n";
		}
	}
	close F;
	close O;
	system("rm hmm output");

	# exit();
}

sub getInfo {
	my %retval;
	open F, $_[0];
	open O, ">>all.GSEs.txt";
	my ( $gse, $gpl, $pdat, $taxon, $suppFile, $summary, $n_samples ) = 'NA';
	while (<F>) {
		s/\r//g;
		s/\!//;
		s/\#//g;

		if (/\/DocSum/) {
			#################################
			# Document summary closed. Print out values
			#################################
			if ( not defined $gses{$gse} ) {
				print O 'GSE'.$gse, "\t", $gpl, "\t", $pdat, "\t", $taxon, "\t", $summary,
					"\t", $suppFile, "\t", $n_samples, "\n";
				$gses{$gse} = 1;
			}
		}
		elsif (/<DocSum/) {
			#################################
			# Document summary opened, reset the variables to NA
			#################################
			( $gse, $gpl, $pdat, $taxon, $summary, $suppFile, $n_samples ) = 'NA';
		}
		#################################
		# Switch to set variables
		#################################
		if (/Name=\"GSE/) {
			/>(\d+)</;
			$gse = $1;
			$gse =~ s/[\'\#]//g;
		}
		if (/Name=\"summary/) {
			/>([^<]+)/;
			$summary = $1;
			$summary =~ s/[\'\#\"]//g;
		}
		if (/Name=\"taxon/) {
			/>([^<]+)/;
			$taxon = $1;
			$taxon =~ s/[\'\#]//g;
		}
		if (/Name=\"PDAT/) {
			/>([^<]+)/;
			$pdat = $1;
			$pdat =~ s/[\'\#]//g;
		}
		if (/Name=\"n_samples/) {
			/>([^<]+)/;
			$n_samples = $1;
			$n_samples =~ s/[\'\#]//g;
		}
		if (/Name=\"suppFile/) {
			/>([^<]+)/;
			my $tmp = $1;
			if ( $tmp =~ /CEL/ ) {
				$suppFile = 'TRUE';
			}
			else {
				$suppFile = 'FALSE';
			}
		}
		if (/Name=\"GPL/) {
			/>([^<]+)/;
			$gpl = $1;
		}
	}
	close O;
}
