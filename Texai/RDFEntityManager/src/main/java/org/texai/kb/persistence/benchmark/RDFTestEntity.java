/*
 * RDFTestEntity.java
 *
 * Created on October 30, 2006, 6:00 PM
 *
 * Description: Contains annotations to test Semantic Object Relational Mapping for
 * a prototype domain class.
 *
 * Copyright (C) 2006 Stephen L. Reed.
 */
package org.texai.kb.persistence.benchmark;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Id;
import javax.persistence.Transient;
import org.joda.time.DateTime;
import org.openrdf.model.URI;
import org.texai.kb.Constants;
import org.texai.kb.persistence.RDFEntity;
import org.texai.kb.persistence.RDFNamespace;
import org.texai.kb.persistence.RDFPersistent;
import org.texai.kb.persistence.RDFProperty;

/**
 *
 * @author reed
 */
@RDFEntity(namespaces = {
  @RDFNamespace(prefix = "texai", namespaceURI = Constants.TEXAI_NAMESPACE),
  @RDFNamespace(prefix = "cyc", namespaceURI = Constants.CYC_NAMESPACE)
//}, subject = "texai:org.texai.kb.persistence.benchmark.RDFTestEntity", context = "texai:TestContext", type = {"cyc:TransportationDeviceType", "cyc:SpatiallyDisjointObjectType"
}, context = "texai:TestContext", type = {"cyc:TransportationDeviceType", "cyc:SpatiallyDisjointObjectType"
}, subClassOf = {"cyc:Scooter", "cyc:Device-UserPowered"
})
public class RDFTestEntity extends AbstractRDFTestEntity implements RDFPersistent {
  /** the default serial version UID */
  private static final long serialVersionUID = 1L;

  // required annotation and field type
  /** the id allocated by the RDF persistence framework */
  @Id
  private URI id;    // NOPMD
  /** the test name field */
  @RDFProperty(predicate = "cyc:prettyString-Canonical")
  private String name;
  /** test boolean field */
  @RDFProperty(trueClass = "texai:TestTrueClass", falseClass = "texai:TestFalseClass")
  private boolean isSomething;
  /** the number of crew members needed to operate this vehicle */
  @RDFProperty(predicate = "cyc:numberOfCrew")
  private int numberOfCrew;
  /** the number of crew members needed to operate this scooter */
  @RDFProperty(predicate = "cyc:maxNbrOfScooterRiders", subPropertyOf = "texai:hasAttribute")
  private Integer maxNbrOfScooterRiders;
  /** the collection of peers */
  @RDFProperty(predicate = "texai:testRDFEntityPeer", subPropertyOf = "cyc:conceptuallyRelated", range = "texai:org.texai.kb.persistence.benchmark.RDFTestEntity")
  private List<RDFTestEntity> myPeers;
  /** a field that should not be persisted */
  @Transient
  private String dontCareField;
  /** a set of entities */
  @RDFProperty(predicate = "texai:favoriteTestRDFEntityPeer", subPropertyOf = "conceptuallyRelated")
  private RDFTestEntity favoriteTestRDFEntityPeer;
  /** the peer associated by an inverse property */
  @RDFProperty(predicate = "texai:favoriteTestRDFEntityPeer", inverse = true)
  private Set<RDFTestEntity> peersHavingMeAsAFavorite;
  /** an array type */
  //@RDFProperty(predicate = "texai:comments_list")
  @RDFProperty
  private String[] comments;
  /** a list type */
  //@RDFProperty(predicate = "texai:integers_list")
  @RDFProperty
  private List<Integer> integers;
  /** an object set type */
  @RDFProperty(predicate = "texai:someInteger")
  private Set<Integer> someIntegers;
  /** a URI set type */
  //@RDFProperty(predicate = "texai:someURI")
  @RDFProperty
  private Set<URI> someURIs;
  /** a test XML datatype XMLSchema.BYTE field */
  //@RDFProperty(predicate = "texai:byteField", subPropertyOf = "hasAttribute")
  @RDFProperty(subPropertyOf = "hasAttribute")
  private byte byteField;
  /** a test XML datatype XMLSchema.UNSIGNED_BYTE field */
  @RDFProperty(predicate = "texai:testUnsignedByteField", subPropertyOf = "hasAttribute")
  private byte unsignedByteField;
  /** a test XML datatype XMLSchema.INT field */
  @RDFProperty(predicate = "texai:testIntField", subPropertyOf = "hasAttribute")
  private int intField;
  /** a test XML datatype XMLSchema.UNSIGNED_INT field */
  @RDFProperty(predicate = "texai:testUnsignedIntField", subPropertyOf = "hasAttribute")
  private int unsignedIntField;
  /** a test XML datatype XMLSchema.LONG field */
  @RDFProperty(predicate = "texai:testLongField", subPropertyOf = "hasAttribute")
  private long longField;
  /** a test XML datatype XMLSchema.UNSIGNEDLONG field */
  @RDFProperty(predicate = "texai:testUnsignedLongField", subPropertyOf = "hasAttribute")
  private long unsignedLongField;
  /** a test XML datatype XMLSchema.FLOAT field */
  @RDFProperty(predicate = "texai:testFloatField", subPropertyOf = "hasAttribute")
  private float floatField;
  /** a test XML datatype XMLSchema.DOUBLE field */
  @RDFProperty(predicate = "texai:testDoubleField", subPropertyOf = "hasAttribute")
  private double doubleField;
  /** a test XML datatype XMLSchema.INTEGER field */
  @RDFProperty(predicate = "texai:testBigIntegerField", subPropertyOf = "hasAttribute")
  private BigInteger bigIntegerField;
  /** a test XML datatype XMLSchema.POSITIVE_INTEGER field */
  @RDFProperty(predicate = "texai:testPositiveBigIntegerField", subPropertyOf = "hasAttribute")
  private BigInteger positiveBigIntegerField;
  /** a test XML datatype XMLSchema.NON_NEGATIVE_INTEGER field */
  @RDFProperty(predicate = "texai:testNonNegativeBigIntegerField", subPropertyOf = "hasAttribute")
  private BigInteger nonNegativeBigIntegerField;
  /** a test XML datatype XMLSchema.NON_POSITIVE_INTEGER field */
  @RDFProperty(predicate = "texai:testNonPositiveBigIntegerField", subPropertyOf = "hasAttribute")
  private BigInteger nonPositiveBigIntegerField;
  /** a test XML datatype XMLSchema.NEGATIVE_INTEGER field */
  @RDFProperty(predicate = "texai:testNegativeBigIntegerField", subPropertyOf = "hasAttribute")
  private BigInteger negativeBigIntegerField;
  /** a test XML datatype XMLSchema.DECIMAL field */
  @RDFProperty(predicate = "texai:testBigDecimalField", subPropertyOf = "hasAttribute")
  private BigDecimal bigDecimalField;
  /** a test XML datatype XMLSchema.DATETIME - Calendar field */
  @RDFProperty(predicate = "texai:testCalendarField", subPropertyOf = "hasAttribute")
  private Calendar calendarField;
  /** a test XML datatype XMLSchema.DATETIME - DateTime field */
  @RDFProperty(predicate = "texai:testDateTimeField", subPropertyOf = "hasAttribute")
  private DateTime dateTimeField;
  /** a test XML datatype XMLSchema.DATETIME - Date field */
  @RDFProperty(predicate = "texai:testDateField", subPropertyOf = "hasAttribute")
  private Date dateField;
  /** a test UUID field */
  @RDFProperty(predicate = "texai:testUUIDField", subPropertyOf = "hasAttribute")
  private UUID uuidField;

  /**
   * Creates a new instance of RDFTestEntity
   */
  public RDFTestEntity() {
    super();
  }

  /** Gets the id allocated by the RDF persistence framework.
   *
   * @return the id allocated by the RDF persistence framework
   */
  @Override
  public URI getId() {
    return id;
  }

  /** Gets the test name field.
   *
   * @return the test name field
   */
  public String getName() {
    return name;
  }

  /** Sets the test name field.
   *
   * @param name the test name field
   */
  public void setName(final String name) {
    this.name = name;
  }

  /** Gets the number of crew members needed to operate this vehicle - test int field.
   *
   * @return the number of crew members needed to operate this vehicle
   */
  public int getNumberOfCrew() {
    return numberOfCrew;
  }

  /** Sets  the number of crew members needed to operate this vehicle - test int field.
   *
   * @param numberOfCrew  the number of crew members needed to operate this vehicle - test int field
   */
  public void setNumberOfCrew(final int numberOfCrew) {
    this.numberOfCrew = numberOfCrew;
  }

  /** Gets the number of crew members needed to operate this scooter - test Integer field.
   *
   * @return the number of crew members needed to operate this scooter
   */
  public int getMaxNbrOfScooterRiders() {
    return maxNbrOfScooterRiders;
  }

  /** Sets the number of crew members needed to operate this scooter - test Integer field.
   *
   * @param maxNbrOfScooterRiders the number of crew members needed to operate this scooter - test Integer field
   */
  public void setMaxNbrOfScooterRiders(final int maxNbrOfScooterRiders) {
    this.maxNbrOfScooterRiders = maxNbrOfScooterRiders;
  }

  /** Gets the collection of peers - test RDF entity list field.
   *
   * @return the collection of peers
   */
  public List<RDFTestEntity> getMyPeers() {
    return myPeers;
  }

  /** Sets the collection of peers - test RDF entity list field.
   *
   * @param myPeers the collection of peers - test RDF entity list field
   */
  public void setMyPeers(final List<RDFTestEntity> myPeers) {
    this.myPeers = myPeers;
  }

  /** Gets a field that should not be persisted - test transient field
   *
   * @return a field that should not be persisted
   */
  public String getDontCareField() {
    return dontCareField;
  }

  /** Sets a field that should not be persisted - test transient field.
   *
   * @param dontCareField a field that should not be persisted - test transient field
   */
  public void setDontCareField(final String dontCareField) {
    this.dontCareField = dontCareField;
  }

  /** Gets the peer associated by an inverse property.
   *
   * @return the peer associated by an inverse property
   */
  public RDFTestEntity getFavoriteTestRDFEntityPeer() {
    return favoriteTestRDFEntityPeer;
  }

  /** Sets the peer associated by an inverse property.
   *
   * @param favoriteTestRDFEntityPeer the peer associated by an inverse property
   */
  public void setFavoriteTestRDFEntityPeer(final RDFTestEntity favoriteTestRDFEntityPeer) {
    this.favoriteTestRDFEntityPeer = favoriteTestRDFEntityPeer;
  }

  /** Returns whether some other object equals this one.
   *
   * @param object the reference object with which to compare
   * @return <code>true</code> if this object is the same as the argument;
   * <code>false</code> otherwise.
   */
  @Override
  public boolean equals(final Object object) {
    if (!(object instanceof RDFTestEntity)) {
      return false;
    }
    final RDFTestEntity that = (RDFTestEntity) object;
    return this.getName().equals(that.getName());
  }

  /** Returns a hash code for this object.
   *
   * @return a hash code for this object
   */
  @Override
  public int hashCode() {
    return this.getName().hashCode();
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return "[TestDomainEntity: " + name + ", id: " + id + "]";
  }

  /** Gets an array type.
   *
   * @return an array type
   */
  public String[] getComment() {
    return comments.clone();
  }

  /** Sets an array type.
   *
   * @param comment an array type
   */
  public void setComment(final String[] comment) {
    this.comments = comment.clone();
  }

  /** Gets a test XML datatype XMLSchema.BYTE field.
   *
   * @return a test XML datatype XMLSchema.BYTE field
   */
  public byte getByteField() {
    return byteField;
  }

  /** Sets a test XML datatype XMLSchema.BYTE field.
   *
   * @param byteField a test XML datatype XMLSchema.BYTE field
   */
  public void setByteField(final byte byteField) {
    this.byteField = byteField;
  }

  /** Gets a test XML datatype XMLSchema.INT field.
   *
   * @return a test XML datatype XMLSchema.INT field
   */
  public int getIntField() {
    return intField;
  }

  /** Sets a test XML datatype XMLSchema.INT field.
   *
   * @param intField a test XML datatype XMLSchema.INT field
   */
  public void setIntField(final int intField) {
    this.intField = intField;
  }

  /** Gets a test XML datatype XMLSchema.LONG field.
   *
   * @return a test XML datatype XMLSchema.LONG field
   */
  public long getLongField() {
    return longField;
  }

  /** Sets a test XML datatype XMLSchema.LONG field.
   *
   * @param longField a test XML datatype XMLSchema.LONG field
   */
  public void setLongField(final long longField) {
    this.longField = longField;
  }

  /** Gets a test XML datatype XMLSchema.FLOAT field.
   *
   * @return a test XML datatype XMLSchema.FLOAT field
   */
  public float getFloatField() {
    return floatField;
  }

  /** Sets a test XML datatype XMLSchema.FLOAT field.
   *
   * @param floatField a test XML datatype XMLSchema.FLOAT field
   */
  public void setFloatField(final float floatField) {
    this.floatField = floatField;
  }

  /** Gets a test XML datatype XMLSchema.DOUBLE field.
   *
   * @return a test XML datatype XMLSchema.DOUBLE field
   */
  public double getDoubleField() {
    return doubleField;
  }

  /** Sets a test XML datatype XMLSchema.DOUBLE field.
   *
   * @param doubleField a test XML datatype XMLSchema.DOUBLE field
   */
  public void setDoubleField(final double doubleField) {
    this.doubleField = doubleField;
  }

  /** Gets a test XML datatype XMLSchema.INTEGER field.
   *
   * @return a test XML datatype XMLSchema.INTEGER field
   */
  public BigInteger getBigIntegerField() {
    return bigIntegerField;
  }

  /** Sets a test XML datatype XMLSchema.INTEGER field.
   *
   * @param bigIntegerField a test XML datatype XMLSchema.INTEGER field
   */
  public void setBigIntegerField(final BigInteger bigIntegerField) {
    this.bigIntegerField = bigIntegerField;
  }

  /** Gets a test XML datatype XMLSchema.DECIMAL field.
   *
   * @return a test XML datatype XMLSchema.DECIMAL field
   */
  public BigDecimal getBigDecimalField() {
    return bigDecimalField;
  }

  /** Sets a test XML datatype XMLSchema.DECIMAL field.
   *
   * @param bigDecimalField a test XML datatype XMLSchema.DECIMAL field
   */
  public void setBigDecimalField(final BigDecimal bigDecimalField) {
    this.bigDecimalField = bigDecimalField;
  }

  /** Gets a test XML datatype XMLSchema.DATETIME - Calendar field.
   *
   * @return a test XML datatype XMLSchema.DATETIME - Calendar field
   */
  public Calendar getCalendarField() {
    return calendarField;
  }

  /** Sets a test XML datatype XMLSchema.DATETIME - Calendar field.
   *
   * @param calendarField a test XML datatype XMLSchema.DATETIME - Calendar field
   */
  public void setCalendarField(final Calendar calendarField) {
    this.calendarField = calendarField;
  }

  /** Gets a test XML datatype XMLSchema.DATETIME - DateTime field.
   *
   * @return a test XML datatype XMLSchema.DATETIME - DateTime field
   */
  public DateTime getDateTimeField() {
    return dateTimeField;
  }

  /** Sets a test XML datatype XMLSchema.DATETIME - DateTime field.
   *
   * @param dateTimeField a test XML datatype XMLSchema.DATETIME - DateTime field
   */
  public void setDateTimeField(final DateTime dateTimeField) {
    this.dateTimeField = dateTimeField;
  }

  /** Gets a test XML datatype XMLSchema.DATETIME - Date field.
   *
   * @return  a test XML datatype XMLSchema.DATETIME - Date field
   */
  public Date getDateField() {
    return (Date) dateField.clone();
  }

  /** Sets a test XML datatype XMLSchema.DATETIME - Date field.
   *
   * @param dateField a test XML datatype XMLSchema.DATETIME - Date field
   */
  public void setDateField(final Date dateField) {
    this.dateField = (Date) dateField.clone();
  }

  /** Gets the peer associated by an inverse property.
   *
   * @return the peer associated by an inverse property
   */
  public Set<RDFTestEntity> getPeersHavingMeAsAFavorite() {
    return peersHavingMeAsAFavorite;
  }

  /** Sets the peer associated by an inverse property.
   *
   * @param peersHavingMeAsAFavorite the peer associated by an inverse property
   */
  public void setPeersHavingMeAsAFavorite(final Set<RDFTestEntity> peersHavingMeAsAFavorite) {
    this.peersHavingMeAsAFavorite = peersHavingMeAsAFavorite;
  }

  /** Gets an object set type.
   *
   * @return an object set type
   */
  public Set<Integer> getSomeIntegers() {
    return someIntegers;
  }

  /** Sets an object set type
   *
   * @param someIntegers an object set type
   */
  public void setSomeIntegers(final Set<Integer> someIntegers) {
    this.someIntegers = someIntegers;
  }

  /** Gets a URI set type.
   *
   * @return a URI set type
   */
  public Set<URI> getSomeURIs() {
    return someURIs;
  }

  /** Sets a URI set type.
   *
   * @param someURIs a URI set type
   */
  public void setSomeURIs(final Set<URI> someURIs) {
    this.someURIs = someURIs;
  }

  /** Gets a test XML datatype XMLSchema.UNSIGNED_BYTE field.
   *
   * @return a test XML datatype XMLSchema.UNSIGNED_BYTE field
   */
  public byte getUnsignedByteField() {
    return unsignedByteField;
  }

  /** Sets a test XML datatype XMLSchema.UNSIGNED_BYTE field.
   *
   * @param unsignedByteField a test XML datatype XMLSchema.UNSIGNED_BYTE field
   */
  public void setUnsignedByteField(final byte unsignedByteField) {
    this.unsignedByteField = unsignedByteField;
  }

  /** Gets a test XML datatype XMLSchema.INT field.
   *
   * @return a test XML datatype XMLSchema.INT field
   */
  public int getUnsignedIntField() {
    return unsignedIntField;
  }

  /** Sets a test XML datatype XMLSchema.INT field.
   *
   * @param unsignedIntField a test XML datatype XMLSchema.INT field
   */
  public void setUnsignedIntField(final int unsignedIntField) {
    this.unsignedIntField = unsignedIntField;
  }

  /** Gets a test XML datatype XMLSchema.LONG field.
   *
   * @return a test XML datatype XMLSchema.LONG field
   */
  public long getUnsignedLongField() {
    return unsignedLongField;
  }

  /** Sets a test XML datatype XMLSchema.LONG field.
   *
   * @param unsignedLongField a test XML datatype XMLSchema.LONG field
   */
  public void setUnsignedLongField(final long unsignedLongField) {
    this.unsignedLongField = unsignedLongField;
  }

  /** Gets a test XML datatype XMLSchema.POSITIVE_INTEGER field.
   *
   * @return a test XML datatype XMLSchema.POSITIVE_INTEGER field
   */
  public BigInteger getPositiveBigIntegerField() {
    return positiveBigIntegerField;
  }

  /** Sets a test XML datatype XMLSchema.POSITIVE_INTEGER field.
   *
   * @param positiveBigIntegerField a test XML datatype XMLSchema.POSITIVE_INTEGER field
   */
  public void setPositiveBigIntegerField(final BigInteger positiveBigIntegerField) {
    this.positiveBigIntegerField = positiveBigIntegerField;
  }

  /** Gets a test XML datatype XMLSchema.NON_NEGATIVE_INTEGER field.
   *
   * @return a test XML datatype XMLSchema.NON_NEGATIVE_INTEGER field
   */
  public BigInteger getNonNegativeBigIntegerField() {
    return nonNegativeBigIntegerField;
  }

  /** Sets a test XML datatype XMLSchema.NON_NEGATIVE_INTEGER field.
   *
   * @param nonNegativeBigIntegerField a test XML datatype XMLSchema.NON_NEGATIVE_INTEGER field
   */
  public void setNonNegativeBigIntegerField(final BigInteger nonNegativeBigIntegerField) {
    this.nonNegativeBigIntegerField = nonNegativeBigIntegerField;
  }

  /** Gets a test XML datatype XMLSchema.NON_POSITIVE_INTEGER field.
   *
   * @return a test XML datatype XMLSchema.NON_POSITIVE_INTEGER field
   */
  public BigInteger getNonPositiveBigIntegerField() {
    return nonPositiveBigIntegerField;
  }

  /** Sets a test XML datatype XMLSchema.NON_POSITIVE_INTEGER field.
   *
   * @param nonPositiveBigIntegerField a test XML datatype XMLSchema.NON_POSITIVE_INTEGER field
   */
  public void setNonPositiveBigIntegerField(final BigInteger nonPositiveBigIntegerField) {
    this.nonPositiveBigIntegerField = nonPositiveBigIntegerField;
  }

  /** Gets a test XML datatype XMLSchema.NEGATIVE_INTEGER field.
   *
   * @return a test XML datatype XMLSchema.NEGATIVE_INTEGER field
   */
  public BigInteger getNegativeBigIntegerField() {
    return negativeBigIntegerField;
  }

  /** Sets a test XML datatype XMLSchema.NEGATIVE_INTEGER field.
   *
   * @param negativeBigIntegerField a test XML datatype XMLSchema.NEGATIVE_INTEGER field
   */
  public void setNegativeBigIntegerField(final BigInteger negativeBigIntegerField) {
    this.negativeBigIntegerField = negativeBigIntegerField;
  }

  /** Gets a test boolean field.
   *
   * @return a test boolean field
   */
  public boolean isSomething() {
    return isSomething;
  }

  /** Sets a test boolean field.
   *
   * @param isSomething a test boolean field
   */
  public void setIsSomething(final boolean isSomething) {
    this.isSomething = isSomething;
  }

  /** Gets a list type.
   *
   * @return a list type
   */
  public List<Integer> getIntegerList() {
    return integers;
  }

  /** Sets a list type.
   *
   * @param integerList a list type
   */
  public void setIntegerList(final List<Integer> integerList) {
    this.integers = integerList;
  }

  /** Gets the test UUID field.
   *
   * @return the test UUID field
   */
  public UUID getUuidField() {
    return uuidField;
  }

  /** Sets the test UUID field.
   *
   * @param uuidField the test UUID field
   */
  public void setUuidField(UUID uuidField) {
    this.uuidField = uuidField;
  }
}
