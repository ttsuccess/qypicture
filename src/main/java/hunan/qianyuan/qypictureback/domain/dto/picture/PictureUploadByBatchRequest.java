package hunan.qianyuan.qypictureback.domain.dto.picture;
import lombok.Data;
import java.util.List;

@Data
public class PictureUploadByBatchRequest {  
  
    /**  
     * 搜索词  
     */  
    private String searchText;  
  
    /**  
     * 抓取数量  
     */  
    private Integer count;

    /**
     * 从多少条开始抓取
     */
   private Integer start;
    /**
     * 名称前缀
     */
    private String namePrefix;
    /**
     * 图片标签
     */
    private List<String> tags;
    /**
     * 图片类型
     */
    private List<String> category;
}
