ALTER TABLE adc_fields
ADD prefix varchar(255);

DO $$
declare
    repertoire_type    INT := (SELECT id from adc_field_type where name = 'repertoire');
    rearrangement_type INT := (SELECT id from adc_field_type where name = 'rearrangement');
BEGIN
    UPDATE adc_fields
    SET prefix = 'aminoacid'
    WHERE id_type = rearrangement_type
        AND (
            name = 'sequence_aa' OR
            name = 'sequence_alignment_aa' OR
            name = 'germline_alignment_aa' OR
            name = 'junction_aa' OR
            name = 'np1_aa' OR
            name = 'np2_aa' OR
            name = 'np3_aa' OR
            name = 'cdr1_aa' OR
            name = 'cdr2_aa' OR
            name = 'cdr3_aa' OR
            name = 'fwr1_aa' OR
            name = 'fwr2_aa' OR
            name = 'fwr3_aa' OR
            name = 'fwr4_aa' OR
            name = 'v_sequence_alignment_aa' OR
            name = 'd_sequence_alignment_aa' OR
            name = 'd2_sequence_alignment_aa' OR
            name = 'j_sequence_alignment_aa' OR
            name = 'c_sequence_alignment_aa' OR
            name = 'v_germline_alignment_aa' OR
            name = 'd_germline_alignment_aa' OR
            name = 'd2_germline_alignment_aa' OR
            name = 'j_germline_alignment_aa' OR
            name = 'c_germline_alignment_aa'
            );
END $$;
