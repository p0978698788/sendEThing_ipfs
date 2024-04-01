package sendeverything.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity // 定義物件將會成為被JPA管理的實體，將映射到資料庫中的表，這裡的表名稱為roles
@Table(name = "roles")
@Data
@NoArgsConstructor
public class Role {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private ERole name;

  public Role(ERole name) {
    this.name = name;
  }

  public Role(String roleUser) {
  }
}