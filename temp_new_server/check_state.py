import sys
filepath = r"E:\Minecraft\Minecraft Modding\RTSbuilding\src\main\java\com\rtsbuilding\rtsbuilding\server\RtsStorageManager.java"
result_path = r"E:\Minecraft\Minecraft Modding\RTSbuilding\temp_new_server\result.txt"

try:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    with open(result_path, 'w', encoding='utf-8') as out:
        out.write(f"File size: {len(content)} chars\n")
        
        # Try to find all methods that still need replacing
        if 'RtsStorageTransfers.returnCarriedToLinked' in content:
            out.write("FOUND: returnCarriedToLinked\n")
        if 'RtsStorageTransfers.quickDropLinkedItem' in content:
            out.write("FOUND: quickDropLinkedItem\n")
        if 'RtsStorageTransfers.importMenuSlotToLinked' in content:
            out.write("FOUND: importMenuSlotToLinked\n")
        if 'RtsStorageTransfers.pickupLinkedToCarried' in content:
            out.write("FOUND: pickupLinkedToCarried\n")
        if 'RtsStorageTransfers.quickMoveLinkedItem' in content:
            out.write("FOUND: quickMoveLinkedItem\n")
        if 'RtsStorageTransfers.fillPlayerInventoryFromLinked' in content:
            out.write("FOUND: fillPlayerInventoryFromLinked\n")
        if 'RtsStorageMining.mine' in content:
            out.write("FOUND:\n")
            idx = content.find('RtsStorageMining.mine')
            out.write(content[max(0,idx-200):idx+200])
        
        out.write("File unchanged.\n")
except Exception as e:
    with open(result_path, 'w', encoding='utf-8') as out:
        out.write(f"Error: {e}\n")
