/*
 * RegisteredUser.java
 *
 * Created on Apr 21, 2009, 12:33:16 PM
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

/**
 *
 * @author Stephen L. Reed.
 */
public interface RegisteredUser {

  /** Gets the first name.
   *
   * @return the first name
   */
  String getFirstName();

  /** Sets the first name.
   *
   * @param firstName the first name
   */
  void setFirstName(final String firstName);

  /** Gets the last name.
   *
   * @return the last name
   */
  String getLastName();

  /** Sets the last name.
   *
   * @param lastName the last name
   */
  void setLastName(final String lastName);

  /** Gets the email address.
   *
   * @return the email address
   */
  String getEmailAddress();

  /** Sets the email address.
   *
   * @param emailAddress the email address
   */
  void setEmailAddress(final String emailAddress);

  /** Gets the user name.
   *
   * @return the user name
   */
  String getUsername();

  /** Sets the user name.
   *
   * @param username the user name
   */
  void setUsername(final String username);

  /** Gets the encrypted password.
   *
   * @return the encrypted password
   */
  String getEncryptedPassword();

  /** Sets the encrypted password.
   *
   * @param encryptedPassword the encrypted password
   */
  void setEncryptedPassword(final String encryptedPassword);
}
