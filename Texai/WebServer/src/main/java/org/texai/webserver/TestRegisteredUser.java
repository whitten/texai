/*
 * TestRegisteredUser.java
 *
 * Created on Apr 21, 2009, 12:34:03 PM
 *
 * Description: .
 *
 * Copyright (C) Apr 21, 2009 Stephen L. Reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.webserver;

import net.jcip.annotations.NotThreadSafe;

/**
 *
 * @author Stephen L. Reed
 */
@NotThreadSafe
public class TestRegisteredUser implements RegisteredUser {

  /** the first name */
  private String firstName;
  /** the last name */
  private String lastName;
  /** the email address */
  private String emailAddress;
  /** the user name */
  private String username;
  /** the encrypted password */
  private String encryptedPassword;


  /** Constructs a new TestRegisteredUser instance.
   *
   * @param firstName the first name
   * @param lastName the last name
   * @param emailAddress the email address
   * @param username the user name
   * @param encryptedPassword the encrypted password
   */
  public TestRegisteredUser(
          final String firstName,
          final String lastName,
          final String emailAddress,
          final String username,
          final String encryptedPassword) {
    //Preconditions
    assert firstName != null : "firstName must not be null";
    assert !firstName.isEmpty() : "firstName must not be empty";
    assert lastName != null : "lastName must not be null";
    assert !lastName.isEmpty() : "lastName must not be empty";
    assert emailAddress != null : "emailAddress must not be null";
    assert !emailAddress.isEmpty() : "emailAddress must not be empty";
    assert username != null : "username must not be null";
    assert !username.isEmpty() : "username must not be empty";
    assert encryptedPassword != null : "encryptedPassword must not be null";
    assert !encryptedPassword.isEmpty() : "encryptedPassword must not be empty";

    this.firstName = firstName;
    this.lastName = lastName;
    this.emailAddress = emailAddress;
    this.username = username;
    this.encryptedPassword = encryptedPassword;
  }

  /** Gets the first name.
   *
   * @return the first name
   */
  @Override
  public String getFirstName() {
    return firstName;
  }

  /** Sets the first name.
   *
   * @param firstName the first name
   */
  @Override
  public void setFirstName(final String firstName) {
    //Preconditions
    assert firstName != null : "firstName must not be null";
    assert !firstName.isEmpty() : "firstName must not be empty";

    this.firstName = firstName;
  }

  /** Gets the last name.
   *
   * @return the last name
   */
  @Override
  public String getLastName() {
    return lastName;
  }

  /** Sets the last name.
   *
   * @param lastName the last name
   */
  @Override
  public void setLastName(final String lastName) {
    //Preconditions
    assert lastName != null : "lastName must not be null";
    assert !lastName.isEmpty() : "lastName must not be empty";

    this.lastName = lastName;
  }

  /** Gets the email address.
   *
   * @return the email address
   */
  @Override
  public String getEmailAddress() {
    return emailAddress;
  }

  /** Sets the email address.
   *
   * @param emailAddress the email address
   */
  @Override
  public void setEmailAddress(final String emailAddress) {
    //Preconditions
    assert emailAddress != null : "emailAddress must not be null";
    assert !emailAddress.isEmpty() : "emailAddress must not be empty";

    this.emailAddress = emailAddress;
  }

  /** Gets the user nam.
   *
   * @return the user nam
   */
  @Override
  public String getUsername() {
    return username;
  }

  /** Sets the user nam.
   *
   * @param username the user nam
   */
  @Override
  public void setUsername(final String username) {
    //Preconditions
    assert username != null : "username must not be null";
    assert !username.isEmpty() : "username must not be empty";

    this.username = username;
  }

  /** Gets the encrypted password.
   *
   * @return the encrypted password
   */
  @Override
  public String getEncryptedPassword() {
    return encryptedPassword;
  }

  /** Sets the encrypted password.
   *
   * @param encryptedPassword the encrypted password
   */
  @Override
  public void setEncryptedPassword(final String encryptedPassword) {
    //Preconditions
    assert encryptedPassword != null : "encryptedPassword must not be null";
    assert !encryptedPassword.isEmpty() : "encryptedPassword must not be empty";

    this.encryptedPassword = encryptedPassword;
  }

  /** Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return "[" + username + "]";
  }
}
