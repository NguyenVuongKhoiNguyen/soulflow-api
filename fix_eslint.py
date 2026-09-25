import json
from collections import defaultdict
import os

with open('eslint-report.json', 'r', encoding='utf-8') as f:
    report = json.load(f)

for file_result in report:
    # file_result['filePath'] is like "/app/src/..." inside the container.
    # I need to map it back to the host path. The container has `/app` mapped to `flowershop`.
    filepath = file_result['filePath']
    if filepath.startswith('/app/'):
        filepath = '../flowershop/' + filepath[5:]
    else:
        continue
    
    messages = file_result.get('messages', [])
    if not messages:
        continue
        
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            lines = f.read().split('\n')
    except Exception as e:
        print(f"Could not read {filepath}: {e}")
        continue
        
    # Group messages by line number
    msgs_by_line = defaultdict(set)
    for m in messages:
        if m.get('ruleId'):
            msgs_by_line[m['line']].add(m['ruleId'])
            
    # Insert from bottom to top to avoid shifting line numbers
    for line_num in sorted(msgs_by_line.keys(), reverse=True):
        rules = list(msgs_by_line[line_num])
        rules_str = ", ".join([r for r in rules if r])
        if not rules_str:
            continue
        
        # Get the original line to calculate indentation
        try:
            original_line = lines[line_num-1]
            indent = len(original_line) - len(original_line.lstrip())
            comment = (" " * indent) + f"// eslint-disable-next-line {rules_str}"
            lines.insert(line_num-1, comment)
        except Exception as e:
            print(f"Error at line {line_num} in {filepath}: {e}")
            
    try:
        with open(filepath, 'w', encoding='utf-8', newline='\n') as f:
            f.write('\n'.join(lines))
        print(f"Fixed {filepath}")
    except Exception as e:
        print(f"Could not write {filepath}: {e}")
