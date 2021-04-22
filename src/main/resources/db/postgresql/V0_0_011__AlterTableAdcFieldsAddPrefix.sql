DO $$
declare
    repertoire_type    INT := (SELECT id from adc_field_type where name = 'repertoire');
    rearrangement_type INT := (SELECT id from adc_field_type where name = 'rearrangement');
BEGIN
    UPDATE adc_fields
    SET prefix = 'sequence'
    WHERE id_type = rearrangement_type
        AND (
            name = 'sequence_id' OR
            name = 'sequence' OR
            name = 'sequence_alignment' OR
            name = 'v_sequence_start' OR
            name = 'v_sequence_end' OR
            name = 'd_sequence_start' OR
            name = 'd_sequence_end' OR
            name = 'd2_sequence_start' OR
            name = 'd2_sequence_end' OR
            name = 'j_sequence_start' OR
            name = 'j_sequence_end' OR
            name = 'v_sequence_alignment' OR
            name = 'd_sequence_alignment' OR
            name = 'd2_sequence_alignment' OR
            name = 'j_sequence_alignment' OR
            name = 'c_sequence_alignment'
            );

    UPDATE adc_fields
    SET prefix = 'call'
    WHERE id_type = rearrangement_type
        AND (
            name = 'v_call' OR
            name = 'd_call' OR
            name = 'd2_call' OR
            name = 'j_call' OR
            name = 'c_call'
            );

    UPDATE adc_fields
    SET prefix = 'others'
    WHERE id_type = rearrangement_type
        AND (
            name = 'rev_comp' OR
            name = 'productive' OR
            name = 'vj_in_frame' OR
            name = 'stop_codon' OR
            name = 'complete_vdj' OR
            name = 'locus' OR
            name = 'germline_alignment' OR
            name = 'junction' OR
            name = 'np1' OR
            name = 'np2' OR
            name = 'np3' OR
            name = 'cdr1' OR
            name = 'cdr2' OR
            name = 'cdr3' OR
            name = 'fwr1' OR
            name = 'fwr2' OR
            name = 'fwr3' OR
            name = 'fwr4' OR
            name = 'v_score' OR
            name = 'v_identity' OR
            name = 'v_support' OR
            name = 'v_cigar' OR
            name = 'd_score' OR
            name = 'd_identity' OR
            name = 'd_support' OR
            name = 'd_cigar' OR
            name = 'd2_score' OR
            name = 'd2_identity' OR
            name = 'd2_support' OR
            name = 'd2_cigar' OR
            name = 'j_score' OR
            name = 'j_identity' OR
            name = 'j_support' OR
            name = 'j_cigar' OR
            name = 'c_score' OR
            name = 'c_identity' OR
            name = 'c_support' OR
            name = 'c_cigar' OR
            name = 'v_germline_start' OR
            name = 'v_germline_end' OR
            name = 'v_alignment_start' OR
            name = 'v_alignment_end' OR
            name = 'd_germline_start' OR
            name = 'd_germline_end' OR
            name = 'd_alignment_start' OR
            name = 'd_alignment_end' OR
            name = 'd2_germline_start' OR
            name = 'd2_germline_end' OR
            name = 'd2_alignment_start' OR
            name = 'd2_alignment_end' OR
            name = 'j_germline_start' OR
            name = 'j_germline_end' OR
            name = 'j_alignment_start' OR
            name = 'j_alignment_end' OR
            name = 'cdr1_start' OR
            name = 'cdr1_end' OR
            name = 'cdr2_start' OR
            name = 'cdr2_end' OR
            name = 'cdr3_start' OR
            name = 'cdr3_end' OR
            name = 'fwr1_start' OR
            name = 'fwr1_end' OR
            name = 'fwr2_start' OR
            name = 'fwr2_end' OR
            name = 'fwr3_start' OR
            name = 'fwr3_end' OR
            name = 'fwr4_start' OR
            name = 'fwr4_end' OR
            name = 'v_germline_alignment' OR
            name = 'd_germline_alignment' OR
            name = 'd2_germline_alignment' OR
            name = 'j_germline_alignment' OR
            name = 'c_germline_alignment' OR
            name = 'junction_length' OR
            name = 'junction_aa_length' OR
            name = 'np1_length' OR
            name = 'np2_length' OR
            name = 'np3_length' OR
            name = 'n1_length' OR
            name = 'n2_length' OR
            name = 'n3_length' OR
            name = 'p3v_length' OR
            name = 'p5d_length' OR
            name = 'p3d_length' OR
            name = 'p5d2_length' OR
            name = 'p3d2_length' OR
            name = 'p5j_length' OR
            name = 'consensus_count' OR
            name = 'duplicate_count' OR
            name = 'cell_id' OR
            name = 'clone_id' OR
            name = 'rearrangement_id' OR
            name = 'repertoire_id' OR
            name = 'sample_processing_id' OR
            name = 'data_processing_id' OR
            name = 'germline_database'
            );
END $$;
