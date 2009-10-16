package org.googlecode.vkontakte_android.service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

import org.googlecode.userapi.Status;
import org.googlecode.vkontakte_android.CGuiTest;
import org.googlecode.vkontakte_android.CSettings;
import org.googlecode.vkontakte_android.database.StatusDao;
import org.googlecode.vkontakte_android.service.ApiCheckingKit.UpdateType;
import org.googlecode.vkontakte_android.database.MessageDao;
import org.googlecode.vkontakte_android.database.UserDao;
import org.googlecode.vkontakte_android.provider.UserapiDatabaseHelper;
import org.googlecode.vkontakte_android.provider.UserapiProvider;
import org.googlecode.userapi.Message;
import org.googlecode.userapi.User;
import org.googlecode.userapi.VkontakteAPI;
import org.googlecode.userapi.VkontakteAPI.privateMessagesTypes;
import org.json.JSONException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class CheckingService extends Service {

    private static String TAG = "VK-Service";
    private Timer m_timer = new Timer();
    private static SharedPreferences s_prefs;
    private List<Thread> threads = Collections.synchronizedList(new LinkedList<Thread>());
    //private boolean m_hasConnection = true;


    public enum contentToUpdate {
        FRIENDS, MESSAGES_ALL, MESSAGES_IN, MESSAGES_OUT, WALL, HISTORY, STATUSES, ALL
    }

    @Override
    public void onCreate() {
        super.onCreate();
        m_binder = new VkontakteServiceBinder(this);

        s_prefs = PreferenceManager.getDefaultSharedPreferences(this);
        ApiCheckingKit.s_ctx = getApplicationContext();
    }

    @Override
    public void onStart(final Intent intent, int startId) {
        doCheck(intent.getIntExtra("action", 1));
    }

    /**
     * Check given content type for updates
     *
     * @param toUpdate - ordinal of contentToUpdate
     */
    void doCheck(final int toUpdate) {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                contentToUpdate what = contentToUpdate.values()[toUpdate];
                Log.d(TAG, "updating " + what + " is starting...");
                try {
                    switch (what) {
                        case FRIENDS:
                            updateFriends();
                            break;
                        case WALL:
                            updateWall();
                            break;
                        case MESSAGES_ALL:
                            updateMessages();
                            break;
                        case MESSAGES_IN:
                            updateInMessages(100);
                            break;
                        case MESSAGES_OUT:
                            updateOutMessages(100); //should be called when user sends messages
                            break;
                        case HISTORY:
                            updateHistory();
                            break;
                        case STATUSES:
                            updateStatuses();
                            break;
                        default:
                            updateStatuses();
                            updateMessages();
                            //updateWall();
                            updateFriends();
                            //updateHistory();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        threads.add(t);
        t.start();
    }

    /**
     * Starts a thread checking api periodically
     */
    private void restartScheduledUpdates() {

        class CheckingTask extends TimerTask {
            @Override
            public void run() {
                Log.d(TAG, "checking by timer");
                try {
                    updateHistory();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        int period = CSettings.getPeriod(getApplicationContext());
        m_timer.scheduleAtFixedRate(new CheckingTask(), 0L, 1000 * 30);
        Log.d(TAG, "Timer with period: " + period);
    }


    // =============== updating methods

    private void updateInMessages(long count) throws IOException, JSONException {
        //todo: use history or friends-like update with save
        VkontakteAPI api = ApiCheckingKit.getApi();
        List<Message> messages = api.getInbox(0, (int) count);
        MessageDao single = null;
        int countNew = 0;
        for (Message m : messages) {
            
        	MessageDao md = new MessageDao(m);
        	if (single==null) {
        		single = md;
        	}
        	Log.d(TAG, "saving message");
            countNew += md.saveOrUpdate(this);
            
        }
        if (countNew > 0)
            UpdatesNotifier.notifyMessages(this, countNew, single);
        getContentResolver().notifyChange(UserapiProvider.MESSAGES_URI, null);
        //TODO get real counter from provider
    }

    private void updateOutMessages(int count) throws IOException, JSONException {
        //todo: use history or friends-like update with save
        VkontakteAPI api = ApiCheckingKit.getApi();
        List<Message> messages = api.getOutbox(0, count);
        for (Message m : messages) {
            MessageDao md = new MessageDao(m);
            Log.d(TAG, "saving outcoming message");
            md.saveOrUpdate(this);
        }
        getContentResolver().notifyChange(UserapiProvider.MESSAGES_URI, null);
    }

    private void updateFriends() throws IOException, JSONException {
        Log.d(TAG, "updating friends:");
        refreshFriends(ApiCheckingKit.getApi(), getApplicationContext());

        Log.d(TAG, "updating new friends:");
        refreshNewFriends(ApiCheckingKit.getApi(), getApplicationContext());
    }

    private void updateMessages() throws IOException, JSONException {
        updateInMessages(100);
        updateOutMessages(100);
    }

    private void updateWall() {
        Log.d(TAG, "updating wall");
        // todo: implement
    }

    private void updateHistory() throws IOException, JSONException {
        Log.d(TAG, "updating history");
        ApiCheckingKit kit = ApiCheckingKit.getInstance();
        VkontakteAPI api = ApiCheckingKit.getApi();
        updateInMessages(api.getChangesHistory().getFriendsCount());
    }

    private void updateStatuses() throws IOException, JSONException {
        Log.d(TAG, "updating statuses");
        VkontakteAPI api = ApiCheckingKit.getApi();
        int updated;
        int start = 0;
        int fetchSize = 100;
        do {
            List<Status> statuses = api.getTimeline(start, fetchSize);
            List<StatusDao> statusDaos = new LinkedList<StatusDao>();
            for (Status status : statuses) {
                StatusDao statusDao = new StatusDao(status.getStatusId(), status.getUserId(), status.getUserName(), status.getDate(), status.getText());
                statusDaos.add(statusDao);
            }
            updated = StatusDao.bulkSaveOrUpdate(getApplicationContext(), statusDaos);
            start += fetchSize;
        } while (updated != 0);
    }

    //todo: use 'partial' lock for instead of synchronized(?)
    private synchronized void refreshFriends(VkontakteAPI api, Context context) throws IOException, JSONException {
        List<User> friends = api.getMyFriends();
        Log.d(TAG, "got users: " + friends.size());
        StringBuilder notIn = new StringBuilder(" ");
        boolean isNew = false;
        for (User user : friends) {
            UserDao userDao = new UserDao(user, isNew, true);
            userDao.saveOrUpdate(context);
            notIn.append(user.getUserId()).append(",");
            Uri useruri = userDao.saveOrUpdate(this);
            //load photo 
            //TODO maybe put this into UserDao 
            if (user.getUserPhotoUrl() != null) {
                Log.d(TAG, "photo: " + user.getUserPhotoUrl());
                byte[] photo = user.getUserPhoto();
                OutputStream os = getContentResolver().openOutputStream(useruri);
                os.write(photo);
                os.close();
            }
            getContentResolver().notifyChange(useruri, null);
        }
        notIn.deleteCharAt(notIn.length() - 1);//remove last ','
        getContentResolver().delete(UserapiProvider.USERS_URI, UserapiDatabaseHelper.KEY_USER_NEW + "=0" + " AND "
                + UserapiDatabaseHelper.KEY_USER_USERID + " NOT IN(" + notIn + ")" + " AND " +
                UserapiDatabaseHelper.KEY_USER_IS_FRIEND + "=1", null);
    }

    //todo: use 'partial' lock for instead of synchronized(?)
    private synchronized void refreshNewFriends(VkontakteAPI api, Context context) throws IOException, JSONException {
        List<User> friends = api.getMyNewFriends();
        Log.d(TAG, "got new users: " + friends.size());
        StringBuilder notIn = new StringBuilder(" ");
        boolean isNew = true;
        for (User user : friends) {
            UserDao userDao = new UserDao(user, isNew, false);
            Uri useruri = userDao.saveOrUpdate(context);
            notIn.append(user.getUserId()).append(",");
            //load photo
            //TODO maybe put this into UserDao
            if (user.getUserPhotoUrl() != null) {
                Log.d(TAG, "photo: " + user.getUserPhotoUrl());
                byte[] photo = user.getUserPhoto();
                OutputStream os = getContentResolver().openOutputStream(useruri);
                os.write(photo);
                os.close();
            }
            getContentResolver().notifyChange(useruri, null);
        }
        notIn.deleteCharAt(notIn.length() - 1);//remove last ','
        getContentResolver().delete(UserapiProvider.USERS_URI, UserapiDatabaseHelper.KEY_USER_NEW + "=1" + " AND " + UserapiDatabaseHelper.KEY_USER_USERID + " NOT IN(" + notIn + ")", null);
    }

//	private void processMessages(ApiCheckingKit kit, Map<UpdateType, Long> res) {
//		long incomingMess = res.get(UpdateType.MESSAGES)
//				- kit.getPreviosUnreadMessNum();
//		Log.d(TAG, "process messages: " + incomingMess);
//		if (incomingMess == 0) // messages count didn't changed since last
//								// checking
//			return;
//
//		if (incomingMess > 0) // new incoming messages
//		{
//			// TODO check this
//			VkontakteAPI api = ApiCheckingKit.getS_api();
//			try {
//				List<Message> mess = api.getPrivateMessages(api.id, 0, 1024,
//						privateMessagesTypes.inbox);
//				Message last = mess.get(mess.size() - 1);
//				if (useNotifications())
//					UpdatesNotifier.notifyMessages(getApplicationContext(),
//							incomingMess, last.getSender().getUserId());
//				kit.setPreviosUnreadMessNum(res.get(UpdateType.MESSAGES));
//
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (JSONException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//
//		} else // some messages were read by another way
//		{
//			kit.setPreviosUnreadMessNum(res.get(UpdateType.MESSAGES));
//		}
//	}
//
//	private void processFriends(ApiCheckingKit kit, Map<UpdateType, Long> res) {
//		long incomingFr = res.get(UpdateType.FRIENDSHIP_REQ)
//				- kit.getPreviosFriendshipRequestsNum();
//
//		if (incomingFr == 0)
//			return;
//
//		if (incomingFr > 0) {
//			// if (useNotifications())
//			// UpdatesNotifier.notify(getApplicationContext(), "New friends: " +
//			// incomingFr, useSound());
//			kit.setPreviosFriendshipRequestsNum(res
//					.get(UpdateType.FRIENDSHIP_REQ));
//		} else {
//			kit.setPreviosFriendshipRequestsNum(res.get(UpdateType.MESSAGES));
//		}
//	}
//
//	private void processPhotoTags(ApiCheckingKit kit, Map<UpdateType, Long> res) {
//		long incomingTags = res.get(UpdateType.TAGS)
//				- kit.getPreviosNewPhotoTagsNum();
//
//		if (incomingTags == 0)
//			return;
//
//		if (incomingTags > 0) {
//			// if (useNotifications())
//			// UpdatesNotifier.notify(getApplicationContext(),
//			// "New photo tags: " + incomingTags, useSound());
//			kit.setPreviosNewPhotoTagsNum(res.get(UpdateType.TAGS));
//		} else {
//			kit.setPreviosNewPhotoTagsNum(res.get(UpdateType.TAGS));
//		}
//	}

    // ========= preferences

    static boolean useSound() {

        return s_prefs.getBoolean("sound", true);
    }

    static boolean usePics() {

        return s_prefs.getBoolean("pics", true);
    }

    static boolean useNotifications() {

        return s_prefs.getBoolean("notif", true);
    }

    static int getRefreshTime() {

        return s_prefs.getInt("period", 30);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "service stopped");

        // stop all running threads
        for (Thread t : threads) {
            if (t.isAlive())
                t.interrupt();
        }
        super.onDestroy();
    }

    // ============ RPC stuff ============================ 

    private IVkontakteService.Stub m_binder;

    @Override
    public IBinder onBind(Intent intent) {
        return m_binder;
    }


}
