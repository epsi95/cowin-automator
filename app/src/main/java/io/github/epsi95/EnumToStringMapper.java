package io.github.epsi95;

public class EnumToStringMapper {

	public static String mapVaccineTypeToString(VaccineType vaccineType){
		switch (vaccineType){
			case COVISHIELD:
				return "COVISHIELD";
			case COVAXIN:
				return "COVAXIN";
			case SPUTNIK_V:
				return "SPUTNIK V";
			default:
				return "UNKNOWN";
		}
	}
}
