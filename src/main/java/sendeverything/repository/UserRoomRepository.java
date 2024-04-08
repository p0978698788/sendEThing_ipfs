package sendeverything.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sendeverything.models.User;
import sendeverything.models.room.Room;
import sendeverything.models.room.UserRoom;

public interface UserRoomRepository extends JpaRepository<UserRoom, Long> {
    UserRoom findByUserAndRoom(User user, Room room);

}
