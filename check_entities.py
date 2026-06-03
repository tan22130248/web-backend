import os
import re

sql_file = "FinalGraduateDB.sql"
with open(sql_file, 'r') as f:
    sql_content = f.read()

tables = {}
for match in re.finditer(r'CREATE TABLE `(\w+)` \((.*?)\) ENGINE=InnoDB', sql_content, re.DOTALL):
    table_name = match.group(1)
    columns_str = match.group(2)
    columns = []
    for line in columns_str.split('\n'):
        line = line.strip()
        if line.startswith('`'):
            col_name = line.split('`')[1]
            columns.append(col_name)
    tables[table_name] = columns

entity_dir = "src/main/java/com/fashion/auth/model"
java_files = [f for f in os.listdir(entity_dir) if f.endswith('.java')]

for file in java_files:
    with open(os.path.join(entity_dir, file), 'r') as f:
        content = f.read()
    
    table_match = re.search(r'@Table\(name\s*=\s*"(\w+)"\)', content)
    if not table_match:
        continue
    table_name = table_match.group(1)
    
    java_columns = []
    # match private fields
    for field_match in re.finditer(r'(?:@Column\([^)]*\)\s*)*(?:@[A-Za-z]+\s*)*private\s+[\w<>]+\s+(\w+)\s*(?:=.*?)?;', content):
        field_name = field_match.group(1)
        
        # Look for @Column(name="xxx") before this field
        # We can extract the chunk of text before the field declaration
        pre_text = content[:field_match.start()]
        last_annotations = pre_text.split(';')[-1]
        
        col_name_match = re.search(r'@Column\(.*name\s*=\s*"(\w+)".*\)', last_annotations)
        join_name_match = re.search(r'@JoinColumn\(.*name\s*=\s*"(\w+)".*\)', last_annotations)
        
        if col_name_match:
            java_columns.append(col_name_match.group(1))
        elif join_name_match:
            java_columns.append(join_name_match.group(1))
        else:
            # camelCase to snake_case
            snake = re.sub(r'(?<!^)(?=[A-Z])', '_', field_name).lower()
            java_columns.append(snake)
            
    if table_name in tables:
        db_cols = set(tables[table_name])
        jv_cols = set(java_columns)
        
        missing_in_java = db_cols - jv_cols
        extra_in_java = jv_cols - db_cols
        
        if missing_in_java or extra_in_java:
            print(f"Table {table_name}:")
            if missing_in_java:
                print(f"  Missing in Java: {missing_in_java}")
            if extra_in_java:
                print(f"  Extra in Java: {extra_in_java}")
            print()

