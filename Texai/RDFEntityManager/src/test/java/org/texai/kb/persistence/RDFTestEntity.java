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
package org.texai.kb.persistence;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Id;
import javax.persistence.Transient;
import org.joda.time.DateTime;
import org.openrdf.model.URI;
import org.texai.kb.Constants;
import org.texai.kb.persistence.benchmark.AbstractRDFTestEntity;

/**
 *
 * @author reed
 */
@RDFEntity(namespaces = {
  @RDFNamespace(prefix = "texai", namespaceURI = Constants.TEXAI_NAMESPACE),
  @RDFNamespace(prefix = "cyc", namespaceURI = Constants.CYC_NAMESPACE)},
        subject = "texai:org.texai.kb.persistence.RDFTestEntity",
        context = "texai:TestContext",
        type = {"cyc:TransportationDeviceType", "cyc:SpatiallyDisjointObjectType"},
        subClassOf = {"cyc:Scooter", "cyc:Device-UserPowered"})
public class RDFTestEntity extends AbstractRDFTestEntity implements RDFPersistent {

  /** the serial version UID */
  private static final long serialVersionUID = 1L;

  // required annotation and field type
  /** the RDF entity id injected by the container */
  @Id
  private URI id;
  /** the name of this instance */
  @RDFProperty(predicate = "cyc:prettyString-Canonical")
  private String name;
  /** test boolean field 1 */
  @RDFProperty(trueClass = "texai:TestTrueClass", falseClass = "texai:TestFalseClass")
  private boolean isSomething;
  /** test boolean field 2 */
  @RDFProperty()
  private boolean isSomethingElse;
  /** the number of crew members needed to operate this vehicle*/
  @RDFProperty(predicate = "cyc:numberOfCrew")
  private int numberOfCrew;
  /** the number of crew members needed to operate this scooter */
  @RDFProperty(predicate = "cyc:maxNbrOfScooterRiders",
          subPropertyOf = "texai:hasAttribute")
  private Integer maxNbrOfScooterRiders;
  /** the collection of peers */
  @RDFProperty(predicate = "texai:testRDFEntityPeer", subPropertyOf = "cyc:conceptuallyRelated", range = "texai:org.texai.kb.persistence.RDFTestEntity")
  private List<RDFTestEntity> myPeers;
  /** a field that should not be persisted */
  @Transient
  private String dontCareField;
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
  // test XML datatype XMLSchema.UNSIGNED_BYTE
  @RDFProperty(predicate = "texai:testUnsignedByteField", subPropertyOf = "hasAttribute")
  private byte unsignedByteField;
  // test XML datatype XMLSchema.INT
  @RDFProperty(predicate = "texai:testIntField", subPropertyOf = "hasAttribute")
  private int intField;
  // test XML datatype XMLSchema.UNSIGNED_INT
  @RDFProperty(predicate = "texai:testUnsignedIntField", subPropertyOf = "hasAttribute")
  private int unsignedIntField;
  // test XML datatype XMLSchema.LONG
  @RDFProperty(predicate = "texai:testLongField", subPropertyOf = "hasAttribute")
  private long longField;
  // test XML datatype XMLSchema.UNSIGNEDLONG
  @RDFProperty(predicate = "texai:testUnsignedLongField", subPropertyOf = "hasAttribute")
  private long unsignedLongField;
  // test XML datatype XMLSchema.FLOAT
  @RDFProperty(predicate = "texai:testFloatField", subPropertyOf = "hasAttribute")
  private float floatField;
  // test XML datatype XMLSchema.DOUBLE
  @RDFProperty(predicate = "texai:testDoubleField", subPropertyOf = "hasAttribute")
  private double doubleField;
  // test XML datatype XMLSchema.INTEGER
  @RDFProperty(predicate = "texai:testBigIntegerField", subPropertyOf = "hasAttribute")
  private BigInteger bigIntegerField;
  // test XML datatype XMLSchema.POSITIVE_INTEGER
  @RDFProperty(predicate = "texai:testPositiveBigIntegerField", subPropertyOf = "hasAttribute")
  private BigInteger positiveBigIntegerField;
  // test XML datatype XMLSchema.NON_NEGATIVE_INTEGER
  @RDFProperty(predicate = "texai:testNonNegativeBigIntegerField", subPropertyOf = "hasAttribute")
  private BigInteger nonNegativeBigIntegerField;
  // test XML datatype XMLSchema.NON_POSITIVE_INTEGER
  @RDFProperty(predicate = "texai:testNonPositiveBigIntegerField", subPropertyOf = "hasAttribute")
  private BigInteger nonPositiveBigIntegerField;
  // test XML datatype XMLSchema.NEGATIVE_INTEGER
  @RDFProperty(predicate = "texai:testNegativeBigIntegerField", subPropertyOf = "hasAttribute")
  private BigInteger negativeBigIntegerField;
  // test XML datatype XMLSchema.DECIMAL
  @RDFProperty(predicate = "texai:testBigDecimalField", subPropertyOf = "hasAttribute")
  private BigDecimal bigDecimalField;
  // test XML datatype XMLSchema.DATETIME - Calendar
  @RDFProperty(predicate = "texai:testCalendarField", subPropertyOf = "hasAttribute")
  private Calendar calendarField;
  // test XML datatype XMLSchema.DATETIME - DateTime
  @RDFProperty(predicate = "texai:testDateTimeField", subPropertyOf = "hasAttribute")
  private DateTime dateTimeField;
  // test XML datatype XMLSchema.DATETIME - Date
  @RDFProperty(predicate = "texai:testDateField", subPropertyOf = "hasAttribute")
  private Date dateField;
  /** a test UUID field */
  @RDFProperty(predicate = "texai:testUUIDField", subPropertyOf = "hasAttribute")
  private UUID uuidField;
  /** a test Map field */
  @RDFProperty(mapKeyType = "java.lang.Integer", mapValueType = "java.lang.String")
  public Map<Integer, String> mapField;

  /**
   * Creates a new instance of RDFTestEntity
   */
  public RDFTestEntity() {
    super();
  }

  @Override
  public URI getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getNumberOfCrew() {
    return numberOfCrew;
  }

  public void setNumberOfCrew(int numberOfCrew) {
    this.numberOfCrew = numberOfCrew;
  }

  public int getMaxNbrOfScooterRiders() {
    return maxNbrOfScooterRiders;
  }

  public void setMaxNbrOfScooterRiders(int maxNbrOfScooterRiders) {
    this.maxNbrOfScooterRiders = maxNbrOfScooterRiders;
  }

  public List<RDFTestEntity> getMyPeers() {
    return myPeers;
  }

  public void setMyPeers(List<RDFTestEntity> myPeers) {
    this.myPeers = myPeers;
  }

  public String getDontCareField() {
    return dontCareField;
  }

  public void setDontCareField(String dontCareField) {
    this.dontCareField = dontCareField;
  }

  public RDFTestEntity getFavoriteTestRDFEntityPeer() {
    return favoriteTestRDFEntityPeer;
  }

  public void setFavoriteTestRDFEntityPeer(RDFTestEntity favoriteTestRDFEntityPeer) {
    this.favoriteTestRDFEntityPeer = favoriteTestRDFEntityPeer;
  }

  /**
   * Determines whether another object is equal to this AtomicTerm.
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

  public String[] getComment() {
    return comments;
  }

  public void setComment(String[] comment) {
    this.comments = comment;
  }

  public byte getByteField() {
    return byteField;
  }

  public void setByteField(byte byteField) {
    this.byteField = byteField;
  }

  public int getIntField() {
    return intField;
  }

  public void setIntField(int intField) {
    this.intField = intField;
  }

  public long getLongField() {
    return longField;
  }

  public void setLongField(long longField) {
    this.longField = longField;
  }

  public float getFloatField() {
    return floatField;
  }

  public void setFloatField(float floatField) {
    this.floatField = floatField;
  }

  public double getDoubleField() {
    return doubleField;
  }

  public void setDoubleField(double doubleField) {
    this.doubleField = doubleField;
  }

  public BigInteger getBigIntegerField() {
    return bigIntegerField;
  }

  public void setBigIntegerField(BigInteger bigIntegerField) {
    this.bigIntegerField = bigIntegerField;
  }

  public BigDecimal getBigDecimalField() {
    return bigDecimalField;
  }

  public void setBigDecimalField(BigDecimal bigDecimalField) {
    this.bigDecimalField = bigDecimalField;
  }

  public Calendar getCalendarField() {
    return calendarField;
  }

  public void setCalendarField(Calendar calendarField) {
    this.calendarField = calendarField;
  }

  public DateTime getDateTimeField() {
    return dateTimeField;
  }

  public void setDateTimeField(DateTime dateTimeField) {
    this.dateTimeField = dateTimeField;
  }

  public Date getDateField() {
    return dateField;
  }

  public void setDateField(Date dateField) {
    this.dateField = dateField;
  }

  public Set<RDFTestEntity> getPeersHavingMeAsAFavorite() {
    return peersHavingMeAsAFavorite;
  }

  public void setPeersHavingMeAsAFavorite(Set<RDFTestEntity> peersHavingMeAsAFavorite) {
    this.peersHavingMeAsAFavorite = peersHavingMeAsAFavorite;
  }

  public Set<Integer> getSomeIntegers() {
    return someIntegers;
  }

  public void setSomeIntegers(Set<Integer> someIntegers) {
    this.someIntegers = someIntegers;
  }

  public Set<URI> getSomeURIs() {
    return someURIs;
  }

  public void setSomeURIs(Set<URI> someURIs) {
    this.someURIs = someURIs;
  }

  public byte getUnsignedByteField() {
    return unsignedByteField;
  }

  public void setUnsignedByteField(byte unsignedByteField) {
    this.unsignedByteField = unsignedByteField;
  }

  public int getUnsignedIntField() {
    return unsignedIntField;
  }

  public void setUnsignedIntField(int unsignedIntField) {
    this.unsignedIntField = unsignedIntField;
  }

  public long getUnsignedLongField() {
    return unsignedLongField;
  }

  public void setUnsignedLongField(long unsignedLongField) {
    this.unsignedLongField = unsignedLongField;
  }

  public BigInteger getPositiveBigIntegerField() {
    return positiveBigIntegerField;
  }

  public void setPositiveBigIntegerField(BigInteger positiveBigIntegerField) {
    this.positiveBigIntegerField = positiveBigIntegerField;
  }

  public BigInteger getNonNegativeBigIntegerField() {
    return nonNegativeBigIntegerField;
  }

  public void setNonNegativeBigIntegerField(BigInteger nonNegativeBigIntegerField) {
    this.nonNegativeBigIntegerField = nonNegativeBigIntegerField;
  }

  public BigInteger getNonPositiveBigIntegerField() {
    return nonPositiveBigIntegerField;
  }

  public void setNonPositiveBigIntegerField(BigInteger nonPositiveBigIntegerField) {
    this.nonPositiveBigIntegerField = nonPositiveBigIntegerField;
  }

  public BigInteger getNegativeBigIntegerField() {
    return negativeBigIntegerField;
  }

  public void setNegativeBigIntegerField(BigInteger negativeBigIntegerField) {
    this.negativeBigIntegerField = negativeBigIntegerField;
  }

  public boolean isSomething() {
    return isSomething;
  }

  public void setIsSomething(boolean isSomething) {
    this.isSomething = isSomething;
  }

  public boolean isSomethingElse() {
    return isSomethingElse;
  }

  public void setIsSomethingElse(boolean isSomethingElse) {
    this.isSomethingElse = isSomethingElse;
  }

  public List<Integer> getIntegerList() {
    return integers;
  }

  public void setIntegerList(List<Integer> integerList) {
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
