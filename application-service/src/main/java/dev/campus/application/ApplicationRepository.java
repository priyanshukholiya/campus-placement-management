package dev.campus.application;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository
  extends JpaRepository<PlacementApplication, String>
{
  boolean existsByStudentIdAndDriveId(String studentId, String driveId);
  List<PlacementApplication> findAllByStudentIdOrderByAppliedAtDesc(
    String studentId
  );
  List<PlacementApplication> findAllByRecruiterUsernameOrderByAppliedAtDesc(
    String recruiterUsername
  );
  List<PlacementApplication> findAllByOrderByAppliedAtDesc();
}
