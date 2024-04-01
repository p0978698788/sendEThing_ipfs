package sendeverything.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sendeverything.models.room.Room;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Boolean existsByRoomCode(String roomCode);
    Room findByRoomCodeAndPassword(String roomCode, String password);
    Room findByRoomCode(String roomCode);
}
