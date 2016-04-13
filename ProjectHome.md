# 0.2.1 #
  * Исправлена ошибка при экспорте в контакты
  * Исправлены различные ошибки, возникающие при полном или частичном  отключении интернета
  * Исправлен и переделан диалог авторизации пользователя
  * Улучшена скорость загрузки приложения, а также короткие зависания при переключении между приложениями

  * Fixed critical bug in contacts export
  * Fixed critical bugs with limited or lost internet connection
  * Fixed and redesigned Login dialog
  * Improved load speed and switching between applications delay

# 0.2 #
### Списки друзей ###
  * Просмотр списка всех друзей с их аватарками
  * Просмотр списка друзей которые сейчас находятся в оналайне
  * Просмотр списка пользователей, которые хотят вас добавить в друзья (отправили вам запрос на добавление в друзья)
### Профили ###
  * Просмотр информации профиля включая аватар
  * Просмотр статусов профиля
### Сообщения ###
  * Отправка сообщений
  * Просмотр входящих сообщений
  * Просмотр исходящих сообщений
  * Просмотр переписки между вами и пользователем
### Новости ###
  * Статусы ваших друзей в виде ленты по времени
  * Автоматические обновления
  * Получение обновлений о количестве новых сообщений и пользователей, которые хотят вас добавить в друзья
### Настройки ###
  * Периодичность автоматических обновлений с возможностью полного их выключения (Ручной режим обновлений)
  * Показ уведомлений в верхней панели
  * Отключение картинок

### Friends List ###
  * Viewing your friends with avatars
  * Viewing your friends that are currently online
  * Viewing user who are send you request to add them to friends
### Profiles ###
  * Profile info with avatar
  * Status updates
### Messages ###
  * Sending messages
  * Viewing incoming messages
  * Viewing sent messages
  * Conversation view with selected user
### Updates ###
  * Friends statuses timeline
  * Periodic updates
  * Getting updates about new incoming messages and friend requests
### Settings ###
  * Periodic updates interval and disabling periodic updates (Manual update mode)
  * Showing notifications in notification bar
  * Disabling pictures

# 0.1.1 - url encode bugfix #
  * passwords with spaces should work now - thanks to vovkab

# v0.1 alpha - first public release: #
  * 2 design drafts(grid and tabbed)
  * some english and russian localization
  * statuses: updates with photos, ability to change own (known userapi issue: status not added, but changed instead and so not visible in updates)
  * friends: all friends, friends requests with counter in old tabbed design (accept/ignore and add/remove is not implemented yet)
  * messages: read and reply, threaded view, notifications in status bar (known issues: no mark as read/delete; html is not escaped; own name is incorrect)
  * settings: implemented but not yet used anywhere and possibly incorrect
  * my page: photo and name download, status update and some 'lorem ipsum' texts
  * profile page: almost nothing but white page with user name (layout ideas wanted!)

### some more known issues: ###
  * not all refreshes are asynchronous
  * auto-update is not yet implemented
  * status update from new UI doesn't work
  * login might fail on mobile network(seems to be issue with userapi)

&lt;wiki:gadget url="http://www.ohloh.net/p/426571/widgets/project\_cocomo.xml" height="240"  border="0" /&gt;