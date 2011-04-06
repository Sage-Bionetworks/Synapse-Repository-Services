#!/bin/bash
./breakout_layers.sh b_cell_interactome hbci
./breakout_layers.sh breast_cancer_nki nki_breast_cancer
./breakout_layers.sh cancer_call_line_panel gsk_cell_lines
./breakout_layers.sh embryonic_stem_cells embryonic_stem_cells
./breakout_layers.sh glioblastoma_tcga tcga_glioblastoma
./breakout_layers.sh harvard_brain_tissue_resource_center hbtrc
./breakout_layers.sh hepatocellular_carcinoma_hongkong hku_net_only
./breakout_layers.sh heterogeneous_stock_mice flint_hsmice
./breakout_layers.sh human_liver_cohort human_liver_cohort
./breakout_layers.sh lfn-kronos-phase_i  myers_ad
./breakout_layers.sh metabric_breast_cancer bcca_net_only
./breakout_layers.sh mouse_model_of_blood_pressure mci_bxa
./breakout_layers.sh mouse_model_of_diet-induced_atherosclerosis BxD
./breakout_layers.sh mouse_model_of_diet-induced_breastcancer pomp_breast_cancer
./breakout_layers.sh mouse_model_of_sexually_dimorphic_atherosclerotic_traits bxh_apoe
./breakout_layers.sh mskcc_prostate_cancer sawyers_prostate_cancer
./breakout_layers.sh sanger_cell_line_project sanger_cell_lines
./breakout_layers.sh tcga_curation_package tcga_curation_package
./breakout_layers.sh yeast_genetic_interactions yeast_genetic_interactions
./generate_md5sums.sh
