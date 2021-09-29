CREATE OR REPLACE FUNCTION pg_temp.get_scope_id(scope_name VARCHAR)
  RETURNS INT AS
$func$
    SELECT id FROM access_scope WHERE "name" = scope_name;
$func$ LANGUAGE sql;

CREATE OR REPLACE FUNCTION pg_temp.get_adc_field_id(field_type VARCHAR, field_name VARCHAR)
  RETURNS INT AS
$func$
    SELECT af.id FROM adc_fields af
    INNER JOIN adc_field_type ft ON af.id_type = ft.id
    WHERE af."name" = field_name AND ft."name" = field_type;
$func$ LANGUAGE sql;

DO $$
DECLARE
    -- Holders for insertion IDs
    template_id  INT;
    template_id_public  INT;
    template_id_stats  INT;
    template_id_raw  INT;
    adc_id INT;
    scope_id INT;

    -- Field mappings
    arr_fields   VARCHAR[] := array[
        ['repertoire', 'repertoire_id', 'public'],
        ['repertoire', 'repertoire_name', 'public'],
        ['repertoire', 'repertoire_description', 'public'],
        ['repertoire', 'study.study_id', 'public'],
        ['repertoire', 'study.study_title', 'public'],
        ['repertoire', 'study.study_type.value', 'public'],
        ['repertoire', 'study.study_type.id', 'public'],
        ['repertoire', 'study.study_description', 'public'],
        ['repertoire', 'study.inclusion_exclusion_criteria', 'public'],
        ['repertoire', 'study.grants', 'public'],
        ['repertoire', 'study.collected_by', 'public'],
        ['repertoire', 'study.lab_name', 'public'],
        ['repertoire', 'study.lab_address', 'public'],
        ['repertoire', 'study.submitted_by', 'public'],
        ['repertoire', 'study.pub_ids', 'public'],
        ['repertoire', 'study.keywords_study', 'public'],
        ['repertoire', 'subject.subject_id', 'public'],
        ['repertoire', 'subject.synthetic', 'public'],
        ['repertoire', 'subject.organism.value', 'public'],
        ['repertoire', 'subject.organism.id', 'public'],
        ['repertoire', 'subject.sex', 'public'],
        ['repertoire', 'subject.age_min', 'public'],
        ['repertoire', 'subject.age_max', 'public'],
        ['repertoire', 'subject.age_unit.value', 'public'],
        ['repertoire', 'subject.age_unit.id', 'public'],
        ['repertoire', 'subject.age_event', 'public'],
        ['repertoire', 'subject.age', 'public'],
        ['repertoire', 'subject.ancestry_population', 'public'],
        ['repertoire', 'subject.ethnicity', 'public'],
        ['repertoire', 'subject.race', 'public'],
        ['repertoire', 'subject.strain_name', 'public'],
        ['repertoire', 'subject.linked_subjects', 'public'],
        ['repertoire', 'subject.link_type', 'public'],
        ['repertoire', 'subject.diagnosis.study_group_description', 'public'],
        ['repertoire', 'subject.diagnosis.disease_diagnosis', 'public'],
        ['repertoire', 'subject.diagnosis.disease_length', 'public'],
        ['repertoire', 'subject.diagnosis.disease_stage', 'public'],
        ['repertoire', 'subject.diagnosis.prior_therapies', 'public'],
        ['repertoire', 'subject.diagnosis.immunogen', 'public'],
        ['repertoire', 'subject.diagnosis.intervention', 'public'],
        ['repertoire', 'subject.diagnosis.medical_history', 'public'],
        ['repertoire', 'sample.sample_id', 'public'],
        ['repertoire', 'sample.sample_type', 'public'],
        ['repertoire', 'sample.tissue', 'public'],
        ['repertoire', 'sample.anatomic_site', 'public'],
        ['repertoire', 'sample.disease_state_sample', 'public'],
        ['repertoire', 'sample.collection_time_point_relative', 'public'],
        ['repertoire', 'sample.collection_time_point_reference', 'public'],
        ['repertoire', 'sample.biomaterial_provider', 'public'],
        ['repertoire', 'sample.tissue_processing', 'public'],
        ['repertoire', 'sample.cell_subset.value', 'public'],
        ['repertoire', 'sample.cell_subset.id', 'public'],
        ['repertoire', 'sample.cell_phenotype', 'public'],
        ['repertoire', 'sample.cell_species.value', 'public'],
        ['repertoire', 'sample.cell_species.id', 'public'],
        ['repertoire', 'sample.single_cell', 'public'],
        ['repertoire', 'sample.cell_number', 'public'],
        ['repertoire', 'sample.cells_per_reaction', 'public'],
        ['repertoire', 'sample.cell_storage', 'public'],
        ['repertoire', 'sample.cell_quality', 'public'],
        ['repertoire', 'sample.cell_isolation', 'public'],
        ['repertoire', 'sample.cell_processing_protocol', 'public'],
        ['repertoire', 'sample.template_class', 'public'],
        ['repertoire', 'sample.template_quality', 'public'],
        ['repertoire', 'sample.template_amount', 'public'],
        ['repertoire', 'sample.library_generation_method', 'public'],
        ['repertoire', 'sample.library_generation_protocol', 'public'],
        ['repertoire', 'sample.library_generation_kit_version', 'public'],
        ['repertoire', 'sample.pcr_target.pcr_target_locus', 'public'],
        ['repertoire', 'sample.pcr_target.forward_pcr_primer_target_location', 'public'],
        ['repertoire', 'sample.pcr_target.reverse_pcr_primer_target_location', 'public'],
        ['repertoire', 'sample.complete_sequences', 'public'],
        ['repertoire', 'sample.physical_linkage', 'public'],
        ['repertoire', 'sample.sequencing_run_id', 'public'],
        ['repertoire', 'sample.total_reads_passing_qc_filter', 'public'],
        ['repertoire', 'sample.sequencing_platform', 'public'],
        ['repertoire', 'sample.sequencing_facility', 'public'],
        ['repertoire', 'sample.sequencing_run_date', 'public'],
        ['repertoire', 'sample.sequencing_kit', 'public'],
        ['repertoire', 'sample.sequencing_files.file_type', 'public'],
        ['repertoire', 'sample.sequencing_files.filename', 'public'],
        ['repertoire', 'sample.sequencing_files.read_direction', 'public'],
        ['repertoire', 'sample.sequencing_files.read_length', 'public'],
        ['repertoire', 'sample.sequencing_files.paired_filename', 'public'],
        ['repertoire', 'sample.sequencing_files.paired_read_direction', 'public'],
        ['repertoire', 'sample.sequencing_files.paired_read_length', 'public'],
        ['repertoire', 'data_processing.data_processing_id', 'public'],
        ['repertoire', 'data_processing.primary_annotation', 'public'],
        ['repertoire', 'data_processing.software_versions', 'public'],
        ['repertoire', 'data_processing.paired_reads_assembly', 'public'],
        ['repertoire', 'data_processing.quality_thresholds', 'public'],
        ['repertoire', 'data_processing.primer_match_cutoffs', 'public'],
        ['repertoire', 'data_processing.collapsing_method', 'public'],
        ['repertoire', 'data_processing.data_processing_protocols', 'public'],
        ['repertoire', 'data_processing.data_processing_files', 'public'],
        ['repertoire', 'data_processing.germline_database', 'public'],
        ['repertoire', 'data_processing.analysis_provenance_id', 'public'],

        ['rearrangement', 'sequence_id', 'public'],
        ['rearrangement', 'sequence', 'public'],
        ['rearrangement', 'sequence_aa', 'public'],
        ['rearrangement', 'rev_comp', 'public'],
        ['rearrangement', 'productive', 'public'],
        ['rearrangement', 'vj_in_frame', 'public'],
        ['rearrangement', 'stop_codon', 'public'],
        ['rearrangement', 'complete_vdj', 'public'],
        ['rearrangement', 'locus', 'public'],
        ['rearrangement', 'v_call', 'public'],
        ['rearrangement', 'd_call', 'public'],
        ['rearrangement', 'd2_call', 'public'],
        ['rearrangement', 'j_call', 'public'],
        ['rearrangement', 'c_call', 'public'],
        ['rearrangement', 'sequence_alignment', 'public'],
        ['rearrangement', 'sequence_alignment_aa', 'public'],
        ['rearrangement', 'germline_alignment', 'public'],
        ['rearrangement', 'germline_alignment_aa', 'public'],
        ['rearrangement', 'junction', 'public'],
        ['rearrangement', 'junction_aa', 'public'],
        ['rearrangement', 'np1', 'public'],
        ['rearrangement', 'np1_aa', 'public'],
        ['rearrangement', 'np2', 'public'],
        ['rearrangement', 'np2_aa', 'public'],
        ['rearrangement', 'np3', 'public'],
        ['rearrangement', 'np3_aa', 'public'],
        ['rearrangement', 'cdr1', 'public'],
        ['rearrangement', 'cdr1_aa', 'public'],
        ['rearrangement', 'cdr2', 'public'],
        ['rearrangement', 'cdr2_aa', 'public'],
        ['rearrangement', 'cdr3', 'public'],
        ['rearrangement', 'cdr3_aa', 'public'],
        ['rearrangement', 'fwr1', 'public'],
        ['rearrangement', 'fwr1_aa', 'public'],
        ['rearrangement', 'fwr2', 'public'],
        ['rearrangement', 'fwr2_aa', 'public'],
        ['rearrangement', 'fwr3', 'public'],
        ['rearrangement', 'fwr3_aa', 'public'],
        ['rearrangement', 'fwr4', 'public'],
        ['rearrangement', 'fwr4_aa', 'public'],
        ['rearrangement', 'v_score', 'public'],
        ['rearrangement', 'v_identity', 'public'],
        ['rearrangement', 'v_support', 'public'],
        ['rearrangement', 'v_cigar', 'public'],
        ['rearrangement', 'd_score', 'public'],
        ['rearrangement', 'd_identity', 'public'],
        ['rearrangement', 'd_support', 'public'],
        ['rearrangement', 'd_cigar', 'public'],
        ['rearrangement', 'd2_score', 'public'],
        ['rearrangement', 'd2_identity', 'public'],
        ['rearrangement', 'd2_support', 'public'],
        ['rearrangement', 'd2_cigar', 'public'],
        ['rearrangement', 'j_score', 'public'],
        ['rearrangement', 'j_identity', 'public'],
        ['rearrangement', 'j_support', 'public'],
        ['rearrangement', 'j_cigar', 'public'],
        ['rearrangement', 'c_score', 'public'],
        ['rearrangement', 'c_identity', 'public'],
        ['rearrangement', 'c_support', 'public'],
        ['rearrangement', 'c_cigar', 'public'],
        ['rearrangement', 'v_sequence_start', 'public'],
        ['rearrangement', 'v_sequence_end', 'public'],
        ['rearrangement', 'v_germline_start', 'public'],
        ['rearrangement', 'v_germline_end', 'public'],
        ['rearrangement', 'v_alignment_start', 'public'],
        ['rearrangement', 'v_alignment_end', 'public'],
        ['rearrangement', 'd_sequence_start', 'public'],
        ['rearrangement', 'd_sequence_end', 'public'],
        ['rearrangement', 'd_germline_start', 'public'],
        ['rearrangement', 'd_germline_end', 'public'],
        ['rearrangement', 'd_alignment_start', 'public'],
        ['rearrangement', 'd_alignment_end', 'public'],
        ['rearrangement', 'd2_sequence_start', 'public'],
        ['rearrangement', 'd2_sequence_end', 'public'],
        ['rearrangement', 'd2_germline_start', 'public'],
        ['rearrangement', 'd2_germline_end', 'public'],
        ['rearrangement', 'd2_alignment_start', 'public'],
        ['rearrangement', 'd2_alignment_end', 'public'],
        ['rearrangement', 'j_sequence_start', 'public'],
        ['rearrangement', 'j_sequence_end', 'public'],
        ['rearrangement', 'j_germline_start', 'public'],
        ['rearrangement', 'j_germline_end', 'public'],
        ['rearrangement', 'j_alignment_start', 'public'],
        ['rearrangement', 'j_alignment_end', 'public'],
        ['rearrangement', 'cdr1_start', 'public'],
        ['rearrangement', 'cdr1_end', 'public'],
        ['rearrangement', 'cdr2_start', 'public'],
        ['rearrangement', 'cdr2_end', 'public'],
        ['rearrangement', 'cdr3_start', 'public'],
        ['rearrangement', 'cdr3_end', 'public'],
        ['rearrangement', 'fwr1_start', 'public'],
        ['rearrangement', 'fwr1_end', 'public'],
        ['rearrangement', 'fwr2_start', 'public'],
        ['rearrangement', 'fwr2_end', 'public'],
        ['rearrangement', 'fwr3_start', 'public'],
        ['rearrangement', 'fwr3_end', 'public'],
        ['rearrangement', 'fwr4_start', 'public'],
        ['rearrangement', 'fwr4_end', 'public'],
        ['rearrangement', 'v_sequence_alignment', 'public'],
        ['rearrangement', 'v_sequence_alignment_aa', 'public'],
        ['rearrangement', 'd_sequence_alignment', 'public'],
        ['rearrangement', 'd_sequence_alignment_aa', 'public'],
        ['rearrangement', 'd2_sequence_alignment', 'public'],
        ['rearrangement', 'd2_sequence_alignment_aa', 'public'],
        ['rearrangement', 'j_sequence_alignment', 'public'],
        ['rearrangement', 'j_sequence_alignment_aa', 'public'],
        ['rearrangement', 'c_sequence_alignment', 'public'],
        ['rearrangement', 'c_sequence_alignment_aa', 'public'],
        ['rearrangement', 'v_germline_alignment', 'public'],
        ['rearrangement', 'v_germline_alignment_aa', 'public'],
        ['rearrangement', 'd_germline_alignment', 'public'],
        ['rearrangement', 'd_germline_alignment_aa', 'public'],
        ['rearrangement', 'd2_germline_alignment', 'public'],
        ['rearrangement', 'd2_germline_alignment_aa', 'public'],
        ['rearrangement', 'j_germline_alignment', 'public'],
        ['rearrangement', 'j_germline_alignment_aa', 'public'],
        ['rearrangement', 'c_germline_alignment', 'public'],
        ['rearrangement', 'c_germline_alignment_aa', 'public'],
        ['rearrangement', 'junction_length', 'public'],
        ['rearrangement', 'junction_aa_length', 'public'],
        ['rearrangement', 'np1_length', 'public'],
        ['rearrangement', 'np2_length', 'public'],
        ['rearrangement', 'np3_length', 'public'],
        ['rearrangement', 'n1_length', 'public'],
        ['rearrangement', 'n2_length', 'public'],
        ['rearrangement', 'n3_length', 'public'],
        ['rearrangement', 'p3v_length', 'public'],
        ['rearrangement', 'p5d_length', 'public'],
        ['rearrangement', 'p3d_length', 'public'],
        ['rearrangement', 'p5d2_length', 'public'],
        ['rearrangement', 'p3d2_length', 'public'],
        ['rearrangement', 'p5j_length', 'public'],
        ['rearrangement', 'consensus_count', 'public'],
        ['rearrangement', 'duplicate_count', 'public'],
        ['rearrangement', 'cell_id', 'public'],
        ['rearrangement', 'clone_id', 'public'],
        ['rearrangement', 'rearrangement_id', 'public'],
        ['rearrangement', 'repertoire_id', 'public'],
        ['rearrangement', 'sample_processing_id', 'public'],
        ['rearrangement', 'data_processing_id', 'public'],
        ['rearrangement', 'germline_database', 'public']
     ];

    i VARCHAR[];
BEGIN
    INSERT INTO templates(name) VALUES ('default') RETURNING id INTO template_id;

    FOREACH i SLICE 1 IN ARRAY arr_fields
       LOOP
          SELECT pg_temp.get_adc_field_id(i[1], i[2]) INTO adc_id;
          SELECT pg_temp.get_scope_id(i[3]) INTO scope_id;

          INSERT INTO template_mappings(id_template, id_adc_field, id_access_scope)
          VALUES (template_id, adc_id, scope_id);
       END LOOP;

   INSERT INTO templates(name) VALUES ('public') RETURNING id INTO template_id_public;

   FOREACH i SLICE 1 IN ARRAY arr_fields
      LOOP
         SELECT pg_temp.get_adc_field_id(i[1], i[2]) INTO adc_id;
         SELECT pg_temp.get_scope_id('public') INTO scope_id;

         INSERT INTO template_mappings(id_template, id_adc_field, id_access_scope)
         VALUES (template_id_public, adc_id, scope_id);
      END LOOP;

     INSERT INTO templates(name) VALUES ('statistics') RETURNING id INTO template_id_stats;

     FOREACH i SLICE 1 IN ARRAY arr_fields
        LOOP
           SELECT pg_temp.get_adc_field_id(i[1], i[2]) INTO adc_id;
           SELECT pg_temp.get_scope_id('statistics') INTO scope_id;

           INSERT INTO template_mappings(id_template, id_adc_field, id_access_scope)
           VALUES (template_id_stats, adc_id, scope_id);
        END LOOP;

    INSERT INTO templates(name) VALUES ('raw_sequence') RETURNING id INTO template_id_raw;
    
    FOREACH i SLICE 1 IN ARRAY arr_fields
       LOOP
          SELECT pg_temp.get_adc_field_id(i[1], i[2]) INTO adc_id;
          SELECT pg_temp.get_scope_id('raw_sequence') INTO scope_id;

          INSERT INTO template_mappings(id_template, id_adc_field, id_access_scope)
          VALUES (template_id_raw, adc_id, scope_id);
       END LOOP;
END $$