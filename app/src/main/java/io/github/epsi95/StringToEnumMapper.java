package io.github.epsi95;

public class StringToEnumMapper {
	public static VaccineType mapStringToVaccinType(String vaccineType){
		switch (vaccineType){
			case "COVISHIELD":
				return VaccineType.COVISHIELD;
			case "COVAXIN":
				return VaccineType.COVAXIN;
			case "SPUTNIK V":
				return VaccineType.SPUTNIK_V;
			default:
				return VaccineType.COVISHIELD;
		}
	}
}
