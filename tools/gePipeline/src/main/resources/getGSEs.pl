use LWP::Simple;

# Load platforms to search
my %platforms;
my %affiliatedPlatforms;
my %allPlatforms;
#open F, "src/main/resources/ncbiGPLIDs" or die("Cannot find file: src/main/resources/ncbiGPLIDs");
open F, "ncbiGPLIDs" or die("Cannot find file: src/main/resources/ncbiGPLIDs");
while (<F>) {
	chomp;
	my @a = split /\t/;
	$platforms{$a[4]}{probe_tab} = $a[0];
	$platforms{$a[4]}{cdf_file} = $a[1];
	$platforms{$a[4]}{platform} = $a[2];
	$platforms{$a[4]}{species} = $a[3];
	$allPlatforms{$a[4]} = 1;
}

# Get all affiliated platforms from NCBI
foreach my $p (keys %platforms) {
	print $p, "\n";
#	next unless $p eq 'GPL570';
	my $url = 'http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc='.$p;
	system("curl -O $url");
	my $file = 'acc.cgi?acc='.$p;
  my $otherGPLs = parseGPL($file);
  foreach my $a (@{$otherGPLs}) {
  	$affiliatedPlatforms{$a} = $p;
  	$allPlatforms{$a} = 1; 
  }
  system("rm $file");
}

my %gses;
open O, ">all.GSEs.txt";
print O "GSE.ID\tGPL\tLast Update Date\tSpecies\tDescription\tSupplementary_File\tNumber_of_Samples\tInvestigator\tPlatform\n";
close O;

# Download and Parse GEO records for inputted platform

foreach my $p ( keys %allPlatforms ) {
	print "Downloading information for $p\n";
	my $a = $affiliatedPlatforms{$p};
#	next unless $p eq 'GPL96' or $a eq 'GPL96';
#	next unless $p eq 'GPL570';

	my $filename = 'output';
	open G, ">:utf8", $filename;
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
#	system("rm hmm output");
#  exit();
}

sub getInfo {
	my %retval;
	open F, $_[0];
	open O, ">>all.GSEs.txt";
	my ( $gse, $gpl, $pdat, $taxon, $suppFile, $summary, $n_samples,  $invest, $plat ) = 'NA';
	while (<F>) {
		s/\r//g;
		s/\!//;
		s/\#//g;
		if (/\/DocSum/) {
			#################################
			# Document summary closed. Print out values
			#################################
			if ( not defined $gses{$gse} ) {
				my @plat;
				$invest = 'NA';
				my $mult=0;
				my @keys = split ";", $gpl;
				foreach my $k (@keys) {
					$k = 'GPL'.$k;
					if(exists $platforms{$k}){ 
						push @plat, $platforms{$k}{platform};
					}elsif(exists $affiliatedPlatforms{$k}){ 
						push @plat, $platforms{$affiliatedPlatforms{$k}}{platform};
					}
				}
				print O 'GSE'.$gse, "\t", $gpl, "\t", $pdat, "\t", $taxon, "\t", $summary,
					"\t", $suppFile, "\t", $n_samples, "\t", $invest, "\t", join(";",@plat), "\n";
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


sub parseGPL {
	my @gpls;
	open F, $_[0];
	while(<F>){ 
	  if(/Alternative\s+to/){ 
	    $h = <F>;
	    $h =~ /GPL\d+/;
	    push @gpls, $&;
	  }
	  if(/Affiliated\s+with/){ 
	    $h = <F>;
	    $h =~ /GPL\d+/;
	    push @gpls, $&;
	  }
	}
	return \@gpls;
}