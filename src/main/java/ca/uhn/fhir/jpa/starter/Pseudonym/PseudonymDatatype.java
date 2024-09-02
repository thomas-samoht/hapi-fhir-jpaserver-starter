package ca.uhn.fhir.jpa.starter.Pseudonym;

import ca.uhn.fhir.model.api.annotation.Child;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import ca.uhn.fhir.util.ElementUtil;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.instance.model.api.ICompositeType;

import java.io.Serial;

/**
 * This is an example of a custom datatype.
 * This is an STU3 example so it extends Type and implements ICompositeType. For
 * DSTU2 it would extend BaseIdentifiableElement and implement ICompositeDatatype.
 */
@DatatypeDef(name = "CustomDatatype")
public class PseudonymDatatype extends Type implements ICompositeType {

	@Serial
	private static final long serialVersionUID = 1L;

	@Child(name = "date", order = 0, min = 1, max = 1)
	private DateTimeType myDate;

	@Child(name = "kittens", order = 1, min = 1, max = 1)
	private StringType myKittens;

	public DateTimeType getDate() {
		if (myDate == null) myDate = new DateTimeType();
		return myDate;
	}

	public StringType getKittens() {
		return myKittens;
	}

	@Override
	public boolean isEmpty() {
		return ElementUtil.isEmpty(myDate, myKittens);
	}

	public PseudonymDatatype setDate(DateTimeType theValue) {
		myDate = theValue;
		return this;
	}

	public PseudonymDatatype setKittens(StringType theKittens) {
		myKittens = theKittens;
		return this;
	}

	@Override
	protected PseudonymDatatype typedCopy() {
		PseudonymDatatype retVal = new PseudonymDatatype();
		super.copyValues(retVal);
		retVal.myDate = myDate;
		return retVal;
	}
}
