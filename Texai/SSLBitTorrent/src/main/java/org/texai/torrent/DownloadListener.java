/*
 * DownloadListener - Notifies that the requested file has been completely downloaded from peers,
 * and that the worker threads have all been terminated.
 *
 * Copyright (C) 2010 Stephen L. Reed (texai.org)
 *
 * This file is part of Snark.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * Revised by Stephen L. Reed, Dec 22, 2009.
 * Reformatted, fixed Checkstyle, Findbugs and PMD violations, and substituted Log4J logger
 * for consistency with the Texai project.
 */

package org.texai.torrent;

import org.texai.torrent.domainEntity.MetaInfo;

/** Notifies that the requested file has been completely downloaded from peers,
 * and that the worker threads have all been terminated.
 *
 * @author reed
 */
public interface DownloadListener {

  /** Receives notification that the associated download has completed.
   *
   * @param metaInfo the meta info
   */
  void downloadCompleted(final MetaInfo metaInfo);
}
