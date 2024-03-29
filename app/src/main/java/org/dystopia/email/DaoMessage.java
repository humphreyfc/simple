package org.dystopia.email;

/*
    This file is part of FairEmail.

    FairEmail is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    FairEmail is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with FairEmail.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2018, Marcel Bokhorst (M66B)
    Copyright 2018, Distopico (dystopia project) <distopico@riseup.net> and contributors
*/

import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface DaoMessage {

  // About 'dummy': "When the min() or max() aggregate functions are used in an aggregate query,
  // all bare columns in the result set take values from the input row which also contains the
  // minimum or maximum."
  // https://www.sqlite.org/lang_select.html

  @Query(
      "SELECT message.*"
          + ", account.name AS accountName, account.color AS accountColor"
          + ", folder.name AS folderName, folder.display AS folderDisplay, folder.type AS folderType"
          + ", COUNT(message.id) AS count"
          + ", SUM(CASE WHEN message.ui_seen"
          + "    OR folder.type = '"
          + EntityFolder.ARCHIVE
          + "'"
          + "    OR folder.type = '"
          + EntityFolder.OUTBOX
          + "'"
          + "    OR folder.type = '"
          + EntityFolder.DRAFTS
          + "' THEN 0 ELSE 1 END) AS unseen"
          + ", SUM(CASE WHEN message.ui_flagged"
          + "    AND NOT folder.type = '"
          + EntityFolder.ARCHIVE
          + "'"
          + "    AND NOT folder.type = '"
          + EntityFolder.OUTBOX
          + "'"
          + "    AND NOT folder.type = '"
          + EntityFolder.DRAFTS
          + "' THEN 0 ELSE 1 END) AS unflagged"
          + ", (SELECT COUNT(a.id) FROM attachment a WHERE a.message = message.id) AS attachments"
          + " FROM message"
          + " JOIN account ON account.id = message.account"
          + " JOIN folder ON folder.id = message.folder"
          + " WHERE account.`synchronize`"
          + " AND (NOT message.ui_hide OR :debug)"
          + " AND NOT ui_found"
          + " AND folder.unified "
          + " GROUP BY account.id, CASE WHEN message.thread IS NULL THEN message.id ELSE message.thread END"
          + " HAVING SUM(unified) > 0"
          + " ORDER BY CASE"
          + "  WHEN 'unread' = :sort THEN NOT message.ui_seen"
          + "  WHEN 'starred' = :sort THEN message.ui_flagged"
          + "  ELSE 0"
          + " END DESC, message.received DESC, message.sent DESC")
  DataSource.Factory<Integer, TupleMessageEx> pagedUnifiedInbox(String sort, boolean debug);

  @Query(
      "SELECT message.*"
          + ", account.name AS accountName, account.color AS accountColor"
          + ", folder.name AS folderName, folder.display AS folderDisplay, folder.type AS folderType"
          + ", COUNT(message.id) AS count"
          + ", SUM(CASE WHEN message.ui_seen"
          + "    OR (folder.id <> :folderId AND folder.type = '"
          + EntityFolder.ARCHIVE
          + "')"
          + "    OR (folder.id <> :folderId AND folder.type = '"
          + EntityFolder.OUTBOX
          + "')"
          + "    OR (folder.id <> :folderId AND folder.type = '"
          + EntityFolder.DRAFTS
          + "') THEN 0 ELSE 1 END) AS unseen"
          + ", SUM(CASE WHEN message.ui_flagged"
          + "    AND NOT (folder.id <> :folderId AND folder.type = '"
          + EntityFolder.ARCHIVE
          + "')"
          + "    AND NOT (folder.id <> :folderId AND folder.type = '"
          + EntityFolder.OUTBOX
          + "')"
          + "    AND NOT (folder.id <> :folderId AND folder.type = '"
          + EntityFolder.DRAFTS
          + "') THEN 0 ELSE 1 END) AS unflagged"
          + ", (SELECT COUNT(a.id) FROM attachment a WHERE a.message = message.id) AS attachments"
          + " FROM message"
          + " JOIN account ON account.id = message.account"
          + " JOIN folder ON folder.id = message.folder"
          + " JOIN folder f ON f.id = :folderId"
          + " WHERE (message.account = f.account OR folder.type = '"
          + EntityFolder.OUTBOX
          + "')"
          + " AND CASE WHEN (:folderType = '"
          + EntityFolder.TRASH
          + "' "
          + "                 OR :folderType = '"
          + EntityFolder.OUTBOX
          + "') "
          + "        THEN 1"
          + "        ELSE folder.id = :folderId"
          + "      END"
          + " AND (NOT message.ui_hide OR :debug)"
          + " AND ui_found = :found"
          + " GROUP BY CASE WHEN message.thread IS NULL THEN message.id ELSE message.thread END"
          + " HAVING SUM(CASE WHEN folder.id = :folderId THEN 1 ELSE 0 END) > 0"
          + " ORDER BY CASE"
          + "  WHEN 'unread' = :sort THEN NOT message.ui_seen"
          + "  WHEN 'starred' = :sort THEN message.ui_flagged"
          + "  ELSE 0"
          + " END DESC, message.received DESC, message.sent DESC")
  DataSource.Factory<Integer, TupleMessageEx> pagedFolder(
      long folderId, String folderType, String sort, boolean found, boolean debug);

  @Query(
      "SELECT message.*"
          + ", account.name AS accountName, account.color AS accountColor"
          + ", folder.name AS folderName, folder.display AS folderDisplay, folder.type AS folderType"
          + ", (SELECT COUNT(m1.id) FROM message m1 WHERE m1.account = message.account AND m1.thread = message.thread AND NOT m1.ui_hide) AS count"
          + ", CASE WHEN message.ui_seen THEN 0 ELSE 1 END AS unseen"
          + ", CASE WHEN message.ui_flagged THEN 0 ELSE 1 END AS unflagged"
          + ", (SELECT COUNT(a.id) FROM attachment a WHERE a.message = message.id) AS attachments"
          + " FROM message"
          + " JOIN account ON account.id = message.account"
          + " JOIN folder ON folder.id = message.folder"
          + " WHERE message.account = :account"
          + " AND message.thread = :thread"
          + " AND (NOT message.ui_hide OR :debug)"
          + " AND NOT (folder.type = '"
          + EntityFolder.TRASH
          + "' AND folder.id <> :folderId)"
          + " ORDER BY CASE"
          + "  WHEN 'unread' = :sort THEN NOT message.ui_seen"
          + "  WHEN 'starred' = :sort THEN message.ui_flagged"
          + "  ELSE 0"
          + " END DESC, message.received DESC, message.sent DESC")
  DataSource.Factory<Integer, TupleMessageEx> pagedThread(
      long account, long folderId, String thread, String sort, boolean debug);

  @Query("SELECT COUNT(id)" + " FROM message" + " WHERE id = :id")
  int countMessage(long id);

  @Query("SELECT *" + " FROM message" + " WHERE id = :id")
  EntityMessage getMessage(long id);

  @Query(
      "SELECT *"
          + " FROM message"
          + " WHERE folder = :folderId"
          + " AND uid = :uid"
          + " AND ui_found = :found")
  EntityMessage getMessageByUid(long folderId, long uid, boolean found);

  @Query("SELECT *" + " FROM message" + " WHERE folder = :folderId" + " AND NOT ui_found")
  List<EntityMessage> getMessageByFolder(long folderId);

  @Query(
      "SELECT *"
          + " FROM message"
          + " WHERE account = :account"
          + " AND thread = :thread"
          + " AND NOT ui_found")
  List<EntityMessage> getMessageByThread(long account, String thread);

  @Query(
      "SELECT message.* FROM message"
          + " JOIN folder ON folder.id = message.folder"
          + " WHERE message.account = :account"
          + " AND (message.msgid = :msgid"
          + " OR message.msgid = :reference)"
          + " AND ui_found = :found")
  List<EntityMessage> getMessageByMsgId(
      long account, String msgid, String reference, boolean found);

  @Query("SELECT * FROM message" + " WHERE folder = :folderId" + " AND ui_seen")
  List<EntityMessage> getMessageSeen(long folderId);

  @Query(
      "SELECT message.*"
          + ", account.name AS accountName, account.color AS accountColor"
          + ", folder.name AS folderName, folder.display AS folderDisplay, folder.type AS folderType"
          + ", (SELECT COUNT(m1.id) FROM message m1 WHERE m1.account = message.account AND m1.thread = message.thread AND NOT m1.ui_hide) AS count"
          + ", CASE WHEN message.ui_seen THEN 0 ELSE 1 END AS unseen"
          + ", CASE WHEN message.ui_flagged THEN 0 ELSE 1 END AS unflagged"
          + ", (SELECT COUNT(a.id) FROM attachment a WHERE a.message = message.id) AS attachments"
          + " FROM message"
          + " JOIN account ON account.id = message.account"
          + " JOIN folder ON folder.id = message.folder"
          + " WHERE message.id = :id")
  LiveData<TupleMessageEx> liveMessage(long id);

  @Query(
      "SELECT message.*"
          + ", account.name AS accountName, account.color AS accountColor"
          + ", folder.name AS folderName, folder.display AS folderDisplay, folder.type AS folderType"
          + " FROM message"
          + " JOIN account ON account.id = message.account"
          + " JOIN folder ON folder.id = message.folder"
          + " WHERE account.`synchronize`"
          + " AND folder.unified"
          + " AND NOT message.ui_seen"
          + " AND NOT message.ui_hide"
          + " AND NOT message.ui_found"
          + " AND NOT message.ui_ignored"
          + " ORDER BY message.received")
  LiveData<List<TupleNotification>> liveUnseenUnified();

  @Query(
      "SELECT uid FROM message"
          + " WHERE folder = :folderId"
          + " AND received >= :received"
          + " AND NOT uid IS NULL"
          + " AND NOT ui_found" /* keep found messages */)
  List<Long> getUids(long folderId, long received);

  @Insert
  long insertMessage(EntityMessage message);

  @Update
  int updateMessage(EntityMessage message);

  @Query("UPDATE message SET uid = :uid WHERE id = :id")
  int setMessageUid(long id, Long uid);

  @Query("UPDATE message SET seen = :seen WHERE id = :id")
  int setMessageSeen(long id, boolean seen);

  @Query("UPDATE message SET ui_seen = :ui_seen WHERE id = :id")
  int setMessageUiSeen(long id, boolean ui_seen);

  @Query("UPDATE message SET flagged = :flagged WHERE id = :id")
  int setMessageFlagged(long id, boolean flagged);

  @Query("UPDATE message SET ui_flagged = :ui_flagged WHERE id = :id")
  int setMessageUiFlagged(long id, boolean ui_flagged);

  @Query("UPDATE message SET ui_hide = :ui_hide WHERE id = :id")
  int setMessageUiHide(long id, boolean ui_hide);

  @Query("UPDATE message SET ui_ignored = :ui_ignored WHERE id = :id")
  int setMessageUiIgnored(long id, boolean ui_ignored);

  @Query("UPDATE message SET error = :error WHERE id = :id")
  int setMessageError(long id, String error);

  @Query("UPDATE message SET content = :content WHERE id = :id")
  int setMessageContent(long id, boolean content);

  @Query("UPDATE message SET headers = :headers WHERE id = :id")
  int setMessageHeaders(long id, String headers);

  @Query("UPDATE message SET stored = :stored WHERE id = :id")
  int setMessageStored(long id, long stored);

  @Query(
      "UPDATE message SET ui_ignored = 1"
          + " WHERE NOT ui_ignored"
          + " AND folder IN (SELECT id FROM folder WHERE type = '"
          + EntityFolder.INBOX
          + "')")
  int ignoreAll();

  @Query("DELETE FROM message WHERE id = :id")
  int deleteMessage(long id);

  @Query("DELETE FROM message WHERE folder = :folderId AND uid = :uid")
  int deleteMessage(long folderId, long uid);

  @Query("DELETE FROM message WHERE folder = :folderId AND NOT uid IS NULL")
  int deleteLocalMessages(long folderId);

  @Query("DELETE FROM message WHERE folder = :folderId AND seen")
  int deleteSeenMessages(long folderId);

  @Query(
      "DELETE FROM message WHERE folder = :folderId AND received < :received AND NOT uid IS NULL")
  int deleteMessagesBefore(long folderId, long received);

  @Query("DELETE FROM message WHERE ui_found")
  int deleteFoundMessages();
}
