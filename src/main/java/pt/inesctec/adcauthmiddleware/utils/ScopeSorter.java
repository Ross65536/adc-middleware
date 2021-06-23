package pt.inesctec.adcauthmiddleware.utils;

import pt.inesctec.adcauthmiddleware.db.dto.TemplateMappingDto;

import java.util.Comparator;

public class ScopeSorter implements Comparator<TemplateMappingDto> {
    @Override
    public int compare (TemplateMappingDto a1, TemplateMappingDto a2) {
        if (a1.getScope() > a2.getScope()) {
            return 1;
        } else if (a1.getScope() < a2.getScope()) {
            return -1;
        } else {
            return 0;
        }
    }
}
