# BootSector

The project walks through creating the Boot Sector for an operating systems. The code is heavily commented and shows the progression of each stage even if a given stage was overwritten by a later one.  

# Example Running 

    nasm -f bin <FILE.asm> -o <FILE.bin>
    
    qemu-system-x86_64 <FILE.bin>
    
# Sources
   https://www.cs.bham.ac.uk/~exr/lectures/opsys/10_11/lectures/os-dev.pdf
