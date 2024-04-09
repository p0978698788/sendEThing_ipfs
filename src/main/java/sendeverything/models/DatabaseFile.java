package sendeverything.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@ToString(exclude = "fileChunks")
@EqualsAndHashCode(exclude = "fileChunks")
@Table(name = "files")
public class DatabaseFile {
	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "uuid2")
	private String id;
	private Long fileSize;

	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;


	private String fileName;
	private String fileId;
	@OneToMany(mappedBy = "databaseFile", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("chunkNumber ASC")
	private List<FileChunk> fileChunks = new ArrayList<>();
	private String verificationCode;

	@OrderBy("timestamp DESC")
	Instant timestamp = Instant.now();

//	private Uploader uploader;




	public DatabaseFile() {

	}

	public DatabaseFile(String fileName, String fileId,Instant timestamp) {
		this.fileName = fileName;
		this.fileId = fileId;

		this.timestamp = timestamp;
	}





	}

