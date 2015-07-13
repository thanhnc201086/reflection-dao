Uses reflection to inspect data POJOs which are then serialized and restored. Depends heavily on proper usage of DataObject design pattern.
If decent coding standards are used (Service interfaces), this kickstarts persistent storage without overhead of maintaining/updating database tables during volatile phase.
When data POJO's are more or less stable, its enough to switch implementations for real DB/ORM solution.

Tip - use with 'a-cake' project for great control of how are data POJO's developed.