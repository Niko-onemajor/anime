import random
import pymysql
from datetime import datetime, timedelta

conn = pymysql.connect(host='localhost', user='root', password='123456', database='anime', charset='utf8mb4')
cursor = conn.cursor()

# Real user IDs (non-test)
USERS = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,22,23,27,28,30]

POSTS = [
    ("进击的巨人最终季完结，大家觉得结局怎么样？", 
     "刚看完最终季，感觉结局太震撼了！艾伦的选择真的让人意难平，三笠最后那一刀看哭了...大家觉得这个结局是悲剧还是救赎？我个人觉得虽然残酷但这就是艾伦想要的结果。"),
    ("推荐几部冷门但超好看的番剧", 
     "最近挖到几部宝藏番：《昭和元禄落语心中》《三月的狮子》《虫师》，虽然画风不是主流，但剧情和音乐真的顶级，有没有人也喜欢这些？看完之后世界观都变了。"),
    ("【讨论】你心中排名前三的动漫是？", 
     "我先来：1. 钢之炼金术师FA 2. 命运石之门 3. CLANNAD。都是经典中的经典，每次重温都有新的感悟。兄弟们亮出你们的TOP3！"),
    ("新番《葬送的芙莉莲》也太好看了吧", 
     "本来以为只是普通的异世界番，结果第一集就给我看哭了。芙莉莲那种对时间流逝的感悟太戳人了，制作也是顶级水准，霸权社yyds！"),
    ("求助：想入坑EVA该从哪部开始看？", 
     "一直听说EVA是神作但没看过，TV版、新剧场版、旧剧场版，观看顺序应该怎么排？求老粉指点！"),
    ("四月新番推荐：这几部不容错过", 
     "四月番已经播了几集了，目前感觉《怪兽8号》《防风铃》《夜晚的水母不会游泳》都挺不错的，大家还有什么推荐的吗？"),
    ("为什么现在的异世界番越来越同质化了？", 
     "感觉最近几年的异世界番都是一个套路：主角被卡车撞死→转生→获得逆天能力→开后宫。有没有什么打破常规的异世界作品推荐？"),
    ("《咒术回战》第二季观后感：涩谷事变太震撼了", 
     "MAPPA的制作真的太顶了，涩谷事变篇的每一集都是电影级别。五条悟被封印那段看得我心都碎了，虎杖的成长也很让人心疼。"),
    ("【投票】你最喜欢的动漫女角色是谁？", 
     "我投三笠一票！又飒又温柔，战斗力爆表还对艾伦一心一意。当然助手克里斯蒂娜和战场原黑仪也很喜欢。大家呢？"),
    ("《鬼灭之刃》炭治郎的温柔到底是优点还是缺点？", 
     "有时候觉得炭治郎太圣母了，对鬼都同情。但转念一想，正是这种温柔才让他能走到最后。大家觉得呢？"),
    ("推荐一部改变你人生的动漫", 
     "对我来说是《CLANNAD After Story》。看完之后第一次理解了父母的不容易，哭得稀里哗啦的。有些作品真的能改变一个人。"),
    ("《间谍过家家》剧场版值得看吗？", 
     "TV版两季都追完了，安妮亚太可爱了！听说剧场版口碑不错，有没有看过的来分享一下感受？不剧透就行！"),
    ("【讨论】动画制作公司哪家最强？", 
     "我个人觉得：MAPPA（咒术、电锯人）、UFOtable（鬼灭、Fate）、霸权社（巨人、芙莉莲）都是顶级。京都动画虽然遭遇不幸但作品质量依然很高。"),
    ("《迷宫饭》这部番为什么这么上头？", 
     "一开始以为是美食番，结果发现是硬核地下城探险+美食+搞笑的神奇组合！莱欧斯对魔物料理的执着太搞笑了，马露希尔的表情包也好用。"),
    ("【吐槽】现在的番剧名称越来越长了", 
     "以前：《钢之炼金术师》《星际牛仔》《虫师》。现在：《关于我被青梅竹马的妹妹当成哥哥这件事》（我编的）。为什么现在的标题都这么长？"),
    ("《死神》千年血战篇动画质量怎么样？", 
     "听说千年血战篇动画制作很精良，和以前TV版相比简直是两个次元。有没有追完的朋友来评价一下？在考虑要不要补。"),
    ("推荐几部适合入宅的新人看的动漫", 
     "如果推荐给刚入宅的朋友：1. 死亡笔记（智斗）2. 钢之炼金术师FA（冒险）3. 你的名字（恋爱）4. 一拳超人（搞笑热血）。这些都很容易入坑。"),
    ("《赛博朋克：边缘行者》看完后劲太大了", 
     "网飞出的这部真的绝了，扳机社的制作加上CDPR的剧本，露西和David的故事太虐了。I Really Want to Stay at Your House 已经循环播放一个月了。"),
    ("【讨论】动画改编和原作漫画哪个更好？", 
     "有些作品动画超越了原作（如鬼灭的动画打斗），有些则是原作更好（如东京喰种）。大家觉得哪些作品动画比漫画好，哪些反过来？"),
    ("《孤独摇滚》会有第二季吗？", 
     "第一季真的太惊喜了，波奇酱的社恐日常既好笑又真实。制作组把各种抽象表现手法玩出花了。有生之年能看到第二季吗？"),
    ("【求助】想找一部老番，只记得部分剧情", 
     "大概是一个关于时间的番，主角可以通过微波炉发送短信到过去改变未来。画风比较老，但剧情烧脑。有人知道是哪部吗？"),
    ("《我推的孩子》第一集就封神了", 
     "星野爱的故事真的让人意想不到，看完第一集整个人都懵了。动画工房的制作太精良了，YOASOBI的主题曲也是神曲。"),
    ("【讨论】你最喜欢的动漫OST是？", 
     "我选《星际牛仔》的OST，菅野洋子yyds！还有《进击的巨人》的配乐也很震撼，泽野弘之的曲风太有辨识度了。"),
    ("《物理魔法使马修》这种搞笑番太解压了", 
     "马修用肌肉解决一切问题的设定太搞笑了，考试篇的泡芙梗笑死我了。这种不用动脑子的搞笑番真的很适合下班后放松看。"),
    ("【讨论】动漫中你最讨厌的反派是谁？", 
     "修·塔克（钢炼）必须上榜！大哥哥...这个梗太痛了。还有格里菲斯（剑风传奇），背叛队友真的不能忍。"),
]

COMMENTS = [
    "写得太好了，说出了我的心声！",
    "完全同意，这部番确实值得反复看。",
    "我也有同感，特别是那个结局。",
    "楼主说的对，但是我觉得还有一点需要补充。",
    "哈哈哈笑死我了，这个比喻太形象了。",
    "作为一个老二次元，我只能说：懂的都懂。",
    "补充一点，音乐的配合也很重要。",
    "这部番的制作水准确实高，经费在燃烧。",
    "说实话，我第一遍没看懂，第二遍才明白。",
    "推荐一个类似的，也是这种风格。",
    "泪目了，看完之后好几天都在想剧情。",
    "我不同意楼主的观点，但尊重你的看法。",
    "制作组真的很用心，细节做得太好了。",
    "这个场景我反复看了十遍，每次都能发现新细节。",
    "虽然很多人喷，但我觉得这部番真的被低估了。",
    "大佬分析得很到位，学到了。",
    "刚入坑，被朋友安利的，果然没让我失望。",
    "OP和ED也都很好听，已经加入歌单了。",
    "制作组真的把原作的精髓都展现出来了。",
    "这部番改变了我对这类题材的看法。",
    "看完之后专门去补了原作漫画，太精彩了。",
    "声优的演绎也很到位，特别是那个关键场景。",
    "确实，这部番的节奏把控得很好，不拖沓。",
    "我觉得这个角色塑造得最成功，层次很丰富。",
    "建议大家看的时候不要跳OP和ED，有彩蛋。",
]

REPLIES = [
    "说得对！我也这么觉得。",
    "+1，完全同意。",
    "哈哈确实，你总结得很到位。",
    "有道理，但我还是觉得原作的更好。",
    "补充一下，原作里还有更多细节哦。",
    "这个观点很有意思，我之前没想到。",
    "对对对！终于有人和我想到一块了。",
    "其实原作里还有更多细节，建议去看看。",
    "我也是这么想的，但不敢说出来哈哈。",
    "分析得很透彻，给你点赞。",
]

random.seed(42)
base_date = datetime(2026, 7, 15, 10, 0, 0)

all_post_ids = []
all_comment_ids = []

print("=== Step 1: Inserting posts ===")
for i, (title, content) in enumerate(POSTS):
    author = random.choice(USERS)
    days_ago = random.randint(0, 15)
    hours = random.randint(8, 22)
    minutes = random.randint(0, 59)
    post_time = base_date + timedelta(days=days_ago, hours=hours-base_date.hour, minutes=minutes)
    
    cursor.execute(
        "INSERT INTO posts (title, content, author_id, create_time, like_count, dislike_count, comment_count, is_test) VALUES (%s, %s, %s, %s, 0, 0, 0, FALSE)",
        (title, content, author, post_time.strftime('%Y-%m-%d %H:%M:%S'))
    )
    post_id = cursor.lastrowid
    all_post_ids.append(post_id)
    print(f"  Post {post_id}: {title[:30]}... by user {author}")

conn.commit()
print(f"Inserted {len(all_post_ids)} posts")

print("\n=== Step 2: Inserting comments ===")
post_comment_map = {}  # post_id -> [comment_ids]
for idx, post_id in enumerate(all_post_ids):
    author = None  # we need to get the actual author
    cursor.execute("SELECT author_id FROM posts WHERE id = %s", (post_id,))
    post_author = cursor.fetchone()[0]
    
    num_comments = random.randint(2, 5)
    post_comment_ids = []
    for j in range(num_comments):
        comment_author = random.choice(USERS)
        if comment_author == post_author and random.random() < 0.7:
            other_users = [u for u in USERS if u != post_author]
            comment_author = random.choice(other_users) if other_users else post_author
        
        comment_text = random.choice(COMMENTS)
        comment_time = base_date + timedelta(days=random.randint(0, 15), hours=random.randint(8, 22), minutes=random.randint(0, 59))
        
        cursor.execute(
            "INSERT INTO comments (post_id, author_id, content, create_time, like_count, dislike_count, is_test) VALUES (%s, %s, %s, %s, 0, 0, FALSE)",
            (post_id, comment_author, comment_text, comment_time.strftime('%Y-%m-%d %H:%M:%S'))
        )
        comment_id = cursor.lastrowid
        all_comment_ids.append(comment_id)
        post_comment_ids.append(comment_id)
    
    # Add replies
    if len(post_comment_ids) >= 2:
        num_replies = random.randint(0, 2)
        for _ in range(num_replies):
            parent_comment_id = random.choice(post_comment_ids)
            reply_author = random.choice(USERS)
            reply_text = random.choice(REPLIES)
            reply_time = base_date + timedelta(days=random.randint(0, 15), hours=random.randint(8, 22), minutes=random.randint(0, 59))
            
            cursor.execute(
                "INSERT INTO comments (post_id, author_id, parent_id, content, create_time, like_count, dislike_count, is_test) VALUES (%s, %s, %s, %s, %s, 0, 0, FALSE)",
                (post_id, reply_author, parent_comment_id, reply_text, reply_time.strftime('%Y-%m-%d %H:%M:%S'))
            )
            comment_id = cursor.lastrowid
            all_comment_ids.append(comment_id)
            post_comment_ids.append(comment_id)
    
    post_comment_map[post_id] = post_comment_ids

conn.commit()
print(f"Inserted {len(all_comment_ids)} comments")

print("\n=== Step 3: Inserting post interactions (likes/dislikes) ===")
post_interaction_count = 0
for pid in all_post_ids:
    num_likes = random.randint(1, 4)
    likers = random.sample(USERS, min(num_likes, len(USERS)))
    for liker in likers:
        interaction_type = 1 if random.random() < 0.85 else 2
        cursor.execute(
            "INSERT INTO forum_post_interactions (post_id, user_id, interaction_type, create_time) VALUES (%s, %s, %s, NOW())",
            (pid, liker, interaction_type)
        )
        post_interaction_count += 1

conn.commit()
print(f"Inserted {post_interaction_count} post interactions")

print("\n=== Step 4: Inserting comment interactions (likes) ===")
comment_interaction_count = 0
liked_pairs = set()
for cid in all_comment_ids:
    if random.random() < 0.5:
        num_likes = random.randint(1, 3)
        likers = random.sample(USERS, min(num_likes, len(USERS)))
        for liker in likers:
            key = (cid, liker)
            if key not in liked_pairs:
                liked_pairs.add(key)
                cursor.execute(
                    "INSERT INTO forum_comment_interactions (comment_id, user_id, interaction_type, create_time) VALUES (%s, %s, 1, NOW())",
                    (cid, liker)
                )
                comment_interaction_count += 1

conn.commit()
print(f"Inserted {comment_interaction_count} comment interactions")

print("\n=== Step 5: Updating post counts ===")
for pid in all_post_ids:
    cursor.execute(
        "UPDATE posts p SET like_count = (SELECT COUNT(*) FROM forum_post_interactions fpi WHERE fpi.post_id = p.id AND fpi.interaction_type = 1), dislike_count = (SELECT COUNT(*) FROM forum_post_interactions fpi WHERE fpi.post_id = p.id AND fpi.interaction_type = 2), comment_count = (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id AND (c.is_test IS NULL OR c.is_test = FALSE)) WHERE p.id = %s",
        (pid,)
    )

conn.commit()
print("Updated post counts")

# Final verification
cursor.execute("SELECT COUNT(*) FROM posts WHERE is_test=0 OR is_test IS NULL")
print(f"\n=== Final Stats ===")
print(f"Non-test posts: {cursor.fetchone()[0]}")
cursor.execute("SELECT COUNT(*) FROM comments WHERE is_test=0 OR is_test IS NULL")
print(f"Non-test comments: {cursor.fetchone()[0]}")
cursor.execute("SELECT COUNT(*) FROM forum_post_interactions")
print(f"Post interactions: {cursor.fetchone()[0]}")
cursor.execute("SELECT COUNT(*) FROM forum_comment_interactions")
print(f"Comment interactions: {cursor.fetchone()[0]}")

conn.close()
print("\nDone!")